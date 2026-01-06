package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneCryptoImplTest {
    @Test
    void encryptDecryptRoundTrip() {
        PhoneCryptoImpl crypto = new PhoneCryptoImpl(properties(keyBytes(32), keyBytes(32)));
        String phone = "+12025550123";

        String encrypted = crypto.encrypt(phone);
        String decrypted = crypto.decrypt(encrypted);

        assertEquals(phone, decrypted);
    }

    @Test
    void decryptRejectsInvalidFormat() {
        PhoneCryptoImpl crypto = new PhoneCryptoImpl(properties(keyBytes(32), keyBytes(32)));

        assertThrows(ValidationException.class, () -> crypto.decrypt("not-base64"));
    }

    @Test
    void hashIsDeterministicAndSensitiveToInput() {
        PhoneCryptoImpl crypto = new PhoneCryptoImpl(properties(keyBytes(32), keyBytes(32)));

        String first = crypto.hash("+12025550123");
        String second = crypto.hash("+12025550123");
        String other = crypto.hash("+12025550124");

        assertEquals(first, second);
        assertNotEquals(first, other);
    }

    @Test
    void rejectInvalidEncryptionKeyLength() {
        assertThrows(ValidationException.class, () -> new PhoneCryptoImpl(properties(keyBytes(32), keyBytes(10))));
    }

    @Test
    void rejectMissingKeys() {
        PhoneCryptoProperties props = new PhoneCryptoProperties();
        props.setHashKey(null);
        props.setEncKey(null);

        assertThrows(ValidationException.class, () -> new PhoneCryptoImpl(props));
    }

    private PhoneCryptoProperties properties(byte[] hashKey, byte[] encKey) {
        PhoneCryptoProperties props = new PhoneCryptoProperties();
        props.setHashKey(Base64.getEncoder().encodeToString(hashKey));
        props.setEncKey(Base64.getEncoder().encodeToString(encKey));
        return props;
    }

    private byte[] keyBytes(int size) {
        return "a".repeat(size).getBytes(StandardCharsets.UTF_8);
    }
}
