package com.aivanouski.im.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aivanouski.im.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthEndpointsIntegrationTests {
    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setupClient() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void authStartReturnsChallenge() throws Exception {
        String phone = testPhone();
        DeviceMaterial device = new DeviceMaterial();

        webTestClient.post()
                .uri("/auth/start")
                .bodyValue(new AuthStartBody(phone, device.deviceId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("otp_required")
                .jsonPath("$.challenge").isNotEmpty()
                .jsonPath("$.otpExpiresAt").isNotEmpty();
    }

    @Test
    void verifyDeviceReturnsTokens() throws Exception {
        String phone = testPhone();
        DeviceMaterial device = new DeviceMaterial();

        JsonNode authStart = startAuth(phone, device);
        String challenge = authStart.get("challenge").asText();
        String otp = testOtp();
        String signature = device.sign(challenge);

        webTestClient.post()
                .uri("/auth/device/verify")
                .bodyValue(new VerifyBody(phone, device.deviceId(), otp, device.publicKeyBase64(), "EC", signature))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty();
    }

    @Test
    void refreshRotatesTokens() throws Exception {
        String phone = testPhone();
        DeviceMaterial device = new DeviceMaterial();

        JsonNode tokens = verify(phone, device);
        String refreshToken = tokens.get("refreshToken").asText();

        JsonNode refreshed = postForJson("/auth/refresh", new RefreshBody(device.deviceId(), refreshToken));

        assertNotNull(refreshed);
        assertNotNull(refreshed.get("accessToken"));
        assertNotNull(refreshed.get("refreshToken"));
        assertNotEquals(refreshToken, refreshed.get("refreshToken").asText());
    }

    @Test
    void createChatRequiresAuth() throws Exception {
        String phone = testPhone();
        DeviceMaterial device = new DeviceMaterial();
        JsonNode tokens = verify(phone, device);
        String accessToken = tokens.get("accessToken").asText();

        webTestClient.post()
                .uri("/chat")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.chatId").isNotEmpty();
    }

    private JsonNode startAuth(String phone, DeviceMaterial device) {
        return postForJson("/auth/start", new AuthStartBody(phone, device.deviceId()));
    }

    private JsonNode verify(String phone, DeviceMaterial device) throws Exception {
        JsonNode authStart = startAuth(phone, device);
        String challenge = authStart.get("challenge").asText();
        String otp = testOtp();
        String signature = device.sign(challenge);

        return postForJson("/auth/device/verify", new VerifyBody(phone, device.deviceId(), otp, device.publicKeyBase64(), "EC", signature));
    }

    private JsonNode postForJson(String uri, Object body) {
        String raw = webTestClient.post()
                .uri(uri)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse response: " + raw, ex);
        }
    }

    private record AuthStartBody(String phone, UUID deviceId) {
    }

    private record VerifyBody(String phone, UUID deviceId, String otp, String devicePublicKey, String publicKeyAlg, String signature) {
    }

    private record RefreshBody(UUID deviceId, String refreshToken) {
    }

    private static final class DeviceMaterial {
        private final UUID deviceId = UUID.randomUUID();
        private final KeyPair keyPair = generateKeyPair();
        private final Base64.Encoder encoder = Base64.getEncoder();

        UUID deviceId() {
            return deviceId;
        }

        String publicKeyBase64() {
            return encoder.encodeToString(keyPair.getPublic().getEncoded());
        }

        String sign(String challenge) throws Exception {
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(keyPair.getPrivate());
            signer.update(challenge.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return encoder.encodeToString(signer.sign());
        }

        private static KeyPair generateKeyPair() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
                generator.initialize(new ECGenParameterSpec("secp256r1"));
                return generator.generateKeyPair();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to generate keypair", ex);
            }
        }
    }

    private static String testPhone() {
        String[] phones = {"+12025550001", "+12025550002", "+12025550003"};
        return phones[ThreadLocalRandom.current().nextInt(phones.length)];
    }

    private static String testOtp() {
        return "000000";
    }
}
