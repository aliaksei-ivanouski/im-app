package com.aivanouski.im.message;

import com.aivanouski.im.chat.infrastructure.persistence.ChatEntity;
import com.aivanouski.im.chat.infrastructure.persistence.ChatJpaRepository;
import com.aivanouski.im.identity.application.auth.PhoneCrypto;
import com.aivanouski.im.identity.application.auth.TokenService;
import com.aivanouski.im.identity.infrastructure.persistence.DeviceEntity;
import com.aivanouski.im.identity.infrastructure.persistence.DeviceJpaRepository;
import com.aivanouski.im.identity.infrastructure.persistence.RefreshTokenJpaRepository;
import com.aivanouski.im.identity.infrastructure.persistence.UserEntity;
import com.aivanouski.im.identity.infrastructure.persistence.UserJpaRepository;
import com.aivanouski.im.message.infrastructure.persistence.MessageJpaRepository;
import com.aivanouski.im.testsupport.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrentMessageSequenceIntegrationTests {
    private static final int USER_COUNT = 100;
    private static final int MESSAGES_PER_USER = 10;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DeviceJpaRepository deviceJpaRepository;

    @Autowired
    private ChatJpaRepository chatJpaRepository;

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PhoneCrypto phoneCrypto;

    private UUID chatId;

    @BeforeEach
    void setUp() {
        messageJpaRepository.deleteAll();
        refreshTokenJpaRepository.deleteAll();
        deviceJpaRepository.deleteAll();
        chatJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        chatId = UUID.randomUUID();
        chatJpaRepository.save(new ChatEntity(chatId, Instant.now()));
    }

    @Test
    void messageChatNIsSequentialUnderConcurrency() throws Exception {
        List<UserSession> sessions = seedUsers();
        List<Integer> sequenceNumbers = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(USER_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(20);

        for (UserSession session : sessions) {
            executor.submit(() -> {
                try {
                    sendMessages(session, sequenceNumbers);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS), "Timed out waiting for websocket sends.");
        executor.shutdownNow();

        int total = USER_COUNT * MESSAGES_PER_USER;
        assertEquals(total, sequenceNumbers.size(), "Missing responses.");
        List<Integer> sorted = new ArrayList<>(sequenceNumbers);
        sorted.sort(Comparator.naturalOrder());
        for (int i = 1; i <= total; i++) {
            assertEquals(i, sorted.get(i - 1));
        }
    }

    private void sendMessages(UserSession session, List<Integer> sink) {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(session.accessToken());
        URI uri = URI.create("ws://localhost:" + port + "/ws/messages");

        Mono<Void> call = client.execute(uri, headers, ws -> {
            Flux<org.springframework.web.reactive.socket.WebSocketMessage> outbound = Flux.range(0, MESSAGES_PER_USER)
                    .concatMap(i -> Mono.delay(Duration.ofMillis(ThreadLocalRandom.current().nextInt(5, 35)))
                            .map(ignore -> ws.textMessage(requestCreate(session.userId(), "payload-" + i))));

            Mono<Void> inbound = ws.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .map(this::extractSequence)
                    .take(MESSAGES_PER_USER)
                    .doOnNext(sink::add)
                    .then();

            return ws.send(outbound).then(inbound);
        });

        call.block(Duration.ofSeconds(30));
    }

    private String requestCreate(UUID userId, String payload) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "create_message");
        root.put("requestId", UUID.randomUUID().toString());
        ObjectNode body = root.putObject("payload");
        body.put("userId", userId.toString());
        body.put("chatId", chatId.toString());
        body.put("payload", payload);
        return root.toString();
    }

    private int extractSequence(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (!"create_message_result".equals(node.get("type").asText())) {
                throw new IllegalStateException("Unexpected response type: " + node);
            }
            if (!"ok".equals(node.get("status").asText())) {
                throw new IllegalStateException("Create failed: " + node);
            }
            return node.get("payload").get("messageChatN").asInt();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse response: " + raw, ex);
        }
    }

    private List<UserSession> seedUsers() {
        List<UserSession> sessions = new ArrayList<>();
        for (int i = 0; i < USER_COUNT; i++) {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            String phone = "+1202555" + String.format("%04d", i);
            UserEntity user = new UserEntity(
                    userId,
                    phoneCrypto.hash(phone),
                    phoneCrypto.encrypt(phone),
                    Instant.now()
            );
            userJpaRepository.save(user);

            DeviceEntity device = new DeviceEntity(deviceId, userId, "pub", "EC", Instant.now());
            device.setVerified(Instant.now());
            deviceJpaRepository.save(device);

            String accessToken = tokenService.createAccessToken(userId, deviceId);
            sessions.add(new UserSession(userId, accessToken));
        }
        return sessions;
    }

    private record UserSession(UUID userId, String accessToken) {
    }
}
