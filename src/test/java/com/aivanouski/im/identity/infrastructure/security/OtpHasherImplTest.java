package com.aivanouski.im.identity.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpHasherImplTest {
    @Test
    void hashIsDeterministic() {
        OtpHasherImpl hasher = new OtpHasherImpl();

        String hash1 = hasher.hash("123456");
        String hash2 = hasher.hash("123456");

        assertEquals(hash1, hash2);
    }

    @Test
    void matchesValidAndInvalid() {
        OtpHasherImpl hasher = new OtpHasherImpl();
        String hash = hasher.hash("123456");

        assertTrue(hasher.matches("123456", hash));
        assertFalse(hasher.matches("000000", hash));
    }
}
