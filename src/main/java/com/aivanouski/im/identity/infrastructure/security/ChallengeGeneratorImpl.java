package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.ChallengeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ChallengeGeneratorImpl implements ChallengeGenerator {
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return BASE64_URL.encodeToString(bytes);
    }
}
