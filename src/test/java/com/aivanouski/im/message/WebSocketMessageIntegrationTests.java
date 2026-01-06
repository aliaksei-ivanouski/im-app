package com.aivanouski.im.message;

import com.aivanouski.im.chat.infrastructure.persistence.ChatEntity;
import com.aivanouski.im.chat.infrastructure.persistence.ChatJpaRepository;
import com.aivanouski.im.identity.infrastructure.persistence.DeviceEntity;
import com.aivanouski.im.identity.infrastructure.persistence.DeviceJpaRepository;
import com.aivanouski.im.message.infrastructure.persistence.MessageJpaRepository;
import com.aivanouski.im.identity.infrastructure.persistence.RefreshTokenJpaRepository;
import com.aivanouski.im.identity.infrastructure.persistence.UserEntity;
import com.aivanouski.im.identity.infrastructure.persistence.UserJpaRepository;
import com.aivanouski.im.identity.application.auth.TokenService;
import com.aivanouski.im.identity.application.auth.PhoneCrypto;
import com.aivanouski.im.testsupport.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketMessageIntegrationTests {
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

    private UUID userId;
    private UUID deviceId;
    private UUID chatId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        messageJpaRepository.deleteAll();
        refreshTokenJpaRepository.deleteAll();
        deviceJpaRepository.deleteAll();
        chatJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        userId = UUID.randomUUID();
        deviceId = UUID.randomUUID();
        chatId = UUID.randomUUID();

        String phone = "+12025550123";
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

        ChatEntity chat = new ChatEntity(chatId, Instant.now());
        chatJpaRepository.save(chat);

        accessToken = tokenService.createAccessToken(userId, deviceId);
    }

    @Test
    void createEditAndListMessages() {
        JsonNode created = sendWs(requestCreate("hello"));
        JsonNode createdPayload = created.get("payload");
        assertEquals("create_message_result", created.get("type").asText());
        assertEquals("ok", created.get("status").asText());
        assertNotNull(createdPayload.get("id").asText());
        assertEquals(1, createdPayload.get("messageChatN").asInt());
        assertEquals(0, createdPayload.get("version").asInt());

        String messageId = createdPayload.get("id").asText();
        int version = createdPayload.get("version").asInt();

        JsonNode edited = sendWs(requestEdit(messageId, version, "edited"));
        JsonNode editedPayload = edited.get("payload");
        assertEquals("edit_message_result", edited.get("type").asText());
        assertEquals("ok", edited.get("status").asText());
        assertEquals("edited", editedPayload.get("payload").asText());
        assertEquals(version + 1, editedPayload.get("version").asInt());

        JsonNode list = sendWs(requestList());
        JsonNode listPayload = list.get("payload");
        assertEquals("list_messages_result", list.get("type").asText());
        assertEquals("ok", list.get("status").asText());
        assertTrue(listPayload.get("items").size() >= 1);
    }

    private ObjectNode requestCreate(String payload) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "create_message");
        root.put("requestId", "req-create");
        ObjectNode body = root.putObject("payload");
        body.put("userId", userId.toString());
        body.put("chatId", chatId.toString());
        body.put("payload", payload);
        return root;
    }

    private ObjectNode requestEdit(String messageId, int version, String payload) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "edit_message");
        root.put("requestId", "req-edit");
        ObjectNode body = root.putObject("payload");
        body.put("messageId", messageId);
        body.put("userId", userId.toString());
        body.put("chatId", chatId.toString());
        body.put("version", version);
        body.put("payload", payload);
        return root;
    }

    private ObjectNode requestList() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "list_messages");
        root.put("requestId", "req-list");
        ObjectNode body = root.putObject("payload");
        body.put("chatId", chatId.toString());
        body.put("userId", userId.toString());
        body.put("page", 0);
        body.put("size", 10);
        return root;
    }

    private JsonNode sendWs(ObjectNode request) {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        URI uri = URI.create("ws://localhost:" + port + "/ws/messages");
        reactor.core.publisher.Sinks.One<String> sink = reactor.core.publisher.Sinks.one();
        Mono<String> response = client.execute(uri, headers, session ->
                session.send(Mono.just(session.textMessage(request.toString())))
                        .thenMany(session.receive().take(1).map(msg -> msg.getPayloadAsText()))
                        .doOnNext(sink::tryEmitValue)
                        .then()
        ).then(sink.asMono());
        String raw = response.block(Duration.ofSeconds(5));
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse websocket response: " + raw, ex);
        }
    }
}
