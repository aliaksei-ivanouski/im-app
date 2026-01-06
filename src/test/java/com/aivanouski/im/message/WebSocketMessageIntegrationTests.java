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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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

    @Test
    void invalidJsonReturnsError() {
        JsonNode response = sendWsRaw("{not-json");
        assertError(response, "invalid_json");
    }

    @Test
    void missingTypeReturnsError() {
        JsonNode response = sendWs(requestWithoutPayload(null, "req-missing-type"));
        assertError(response, "invalid_request");
        assertEquals("req-missing-type", response.get("requestId").asText());
    }

    @Test
    void unknownTypeReturnsError() {
        JsonNode response = sendWs(requestWithoutPayload("nope", "req-unknown"));
        assertError(response, "unknown_type");
        assertEquals("req-unknown", response.get("requestId").asText());
    }

    @Test
    void createMessageRejectsMissingPayload() {
        JsonNode response = sendWs(requestWithoutPayload("create_message", "req-missing-payload"));
        assertError(response, "validation_error");
    }

    @Test
    void createMessageRejectsMissingUserId() {
        JsonNode response = sendWs(requestCreate(null, chatId, "hello", "req-missing-user"));
        assertError(response, "validation_error");
    }

    @Test
    void createMessageRejectsBlankPayload() {
        JsonNode response = sendWs(requestCreate(userId, chatId, "   ", "req-blank"));
        assertError(response, "validation_error");
    }

    @Test
    void editMessageRejectsMissingVersion() {
        JsonNode created = sendWs(requestCreate("hello"));
        String messageId = created.get("payload").get("id").asText();

        JsonNode response = sendWs(requestEdit(userId, chatId, messageId, null, "edited"));
        assertError(response, "validation_error");
    }

    @Test
    void editMessageRejectsOptimisticLockMismatch() {
        JsonNode created = sendWs(requestCreate("hello"));
        JsonNode createdPayload = created.get("payload");
        String messageId = createdPayload.get("id").asText();
        int version = createdPayload.get("version").asInt();

        JsonNode response = sendWs(requestEdit(userId, chatId, messageId, version + 1, "edited"));
        assertError(response, "optimistic_lock_failed");
    }

    @Test
    void listMessagesRejectsMissingChatId() {
        JsonNode response = sendWs(requestList(null, userId, 0, 10, "req-list-missing-chat"));
        assertError(response, "validation_error");
    }

    @Test
    void listMessagesRejectsInvalidSize() {
        JsonNode response = sendWs(requestList(chatId, userId, 0, 50, "req-list-bad-size"));
        assertError(response, "validation_error");
    }

    @Test
    void keepsConnectionOnMissingChat() {
        ObjectNode badCreate = requestCreate(UUID.randomUUID(), "missing");
        ObjectNode goodList = requestList();

        List<JsonNode> responses = sendWsBatch(badCreate, goodList);

        JsonNode error = responses.stream()
                .filter(node -> "error".equals(node.get("type").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected error response"));
        assertEquals("error", error.get("status").asText());
        assertEquals("validation_error", error.get("error").get("code").asText());

        JsonNode list = responses.stream()
                .filter(node -> "list_messages_result".equals(node.get("type").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected list response"));
        assertEquals("ok", list.get("status").asText());
    }

    private ObjectNode requestCreate(String payload) {
        return requestCreate(chatId, payload);
    }

    private ObjectNode requestCreate(UUID targetChatId, String payload) {
        return requestCreate(userId, targetChatId, payload, "req-create");
    }

    private ObjectNode requestCreate(UUID targetUserId, UUID targetChatId, String payload, String requestId) {
        ObjectNode root = baseRequest("create_message", requestId);
        ObjectNode body = root.putObject("payload");
        if (targetUserId != null) {
            body.put("userId", targetUserId.toString());
        }
        if (targetChatId != null) {
            body.put("chatId", targetChatId.toString());
        }
        if (payload != null) {
            body.put("payload", payload);
        }
        return root;
    }

    private ObjectNode requestEdit(String messageId, int version, String payload) {
        return requestEdit(userId, chatId, messageId, version, payload);
    }

    private ObjectNode requestEdit(UUID targetUserId, UUID targetChatId, String messageId, Integer version, String payload) {
        ObjectNode root = baseRequest("edit_message", "req-edit");
        ObjectNode body = root.putObject("payload");
        if (messageId != null) {
            body.put("messageId", messageId);
        }
        if (targetUserId != null) {
            body.put("userId", targetUserId.toString());
        }
        if (targetChatId != null) {
            body.put("chatId", targetChatId.toString());
        }
        if (version != null) {
            body.put("version", version);
        }
        if (payload != null) {
            body.put("payload", payload);
        }
        return root;
    }

    private ObjectNode requestList() {
        return requestList(chatId, userId, 0, 10, "req-list");
    }

    private ObjectNode requestList(UUID targetChatId, UUID targetUserId, int page, int size, String requestId) {
        ObjectNode root = baseRequest("list_messages", requestId);
        ObjectNode body = root.putObject("payload");
        if (targetChatId != null) {
            body.put("chatId", targetChatId.toString());
        }
        if (targetUserId != null) {
            body.put("userId", targetUserId.toString());
        }
        body.put("page", page);
        body.put("size", size);
        return root;
    }

    private ObjectNode requestWithoutPayload(String type, String requestId) {
        return baseRequest(type, requestId);
    }

    private ObjectNode baseRequest(String type, String requestId) {
        ObjectNode root = objectMapper.createObjectNode();
        if (type != null) {
            root.put("type", type);
        }
        if (requestId != null) {
            root.put("requestId", requestId);
        }
        return root;
    }

    private JsonNode sendWs(ObjectNode request) {
        return sendWsBatch(request).getFirst();
    }

    private List<JsonNode> sendWsBatch(ObjectNode... requests) {
        String[] raw = Arrays.stream(requests).map(ObjectNode::toString).toArray(String[]::new);
        return sendWsBatchRaw(raw);
    }

    private JsonNode sendWsRaw(String raw) {
        return sendWsBatchRaw(raw).getFirst();
    }

    private List<JsonNode> sendWsBatchRaw(String... requests) {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        URI uri = URI.create("ws://localhost:" + port + "/ws/messages");
        reactor.core.publisher.Sinks.One<List<String>> sink = reactor.core.publisher.Sinks.one();
        Mono<List<String>> response = client.execute(uri, headers, session ->
                session.send(Flux.fromArray(requests).map(session::textMessage))
                        .thenMany(session.receive().take(requests.length).map(msg -> msg.getPayloadAsText()))
                        .collectList()
                        .doOnNext(sink::tryEmitValue)
                        .then()
        ).then(sink.asMono());
        List<String> rawList = response.block(Duration.ofSeconds(5));
        if (rawList == null || rawList.size() != requests.length) {
            throw new IllegalStateException("Expected " + requests.length + " websocket responses, got " +
                    (rawList == null ? 0 : rawList.size()));
        }
        return rawList.stream().map(raw -> {
            try {
                return objectMapper.readTree(raw);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to parse websocket response: " + raw, ex);
            }
        }).toList();
    }

    private void assertError(JsonNode response, String code) {
        assertEquals("error", response.get("type").asText());
        assertEquals("error", response.get("status").asText());
        assertEquals(code, response.get("error").get("code").asText());
    }
}
