package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.TokenClaims;
import com.aivanouski.im.identity.application.auth.TokenService;
import com.aivanouski.im.shared.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JwtService implements TokenService {
    private static final String HMAC_ALG = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.secureRandom = new SecureRandom();
        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalStateException("security.jwt.secret must be set.");
        }
    }

    public String createAccessToken(UUID userId, UUID deviceId) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId.toString());
        claims.put("device_id", deviceId.toString());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(properties.getAccessTtlSeconds()).getEpochSecond());
        if (properties.getIssuer() != null && !properties.getIssuer().isBlank()) {
            claims.put("iss", properties.getIssuer());
        }
        return sign(claims);
    }

    public String createRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return BASE64_URL.encodeToString(bytes);
    }

    public TokenClaims verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ValidationException("Invalid token.");
        }
        String signature = signRaw(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(signature, parts[2])) {
            throw new ValidationException("Invalid token signature.");
        }
        String payloadJson = new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, Object> payload = readJson(payloadJson);
        Object expObj = payload.get("exp");
        if (!(expObj instanceof Number)) {
            throw new ValidationException("Invalid token expiration.");
        }
        long exp = ((Number) expObj).longValue();
        if (Instant.now().getEpochSecond() >= exp) {
            throw new ValidationException("Token expired.");
        }
        UUID userId = parseUuid(payload, "sub");
        UUID deviceId = parseUuid(payload, "device_id");
        return new TokenClaims(userId, deviceId, exp);
    }

    private String sign(Map<String, Object> payload) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = writeJson(payload);
        String header = BASE64_URL.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String body = BASE64_URL.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = signRaw(header + "." + body);
        return header + "." + body + "." + signature;
    }

    private String signRaw(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL.encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign token.", ex);
        }
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Invalid token payload.");
        }
    }

    private UUID parseUuid(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof String str)) {
            throw new ValidationException("Invalid token payload.");
        }
        return UUID.fromString(str);
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to encode token.");
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
