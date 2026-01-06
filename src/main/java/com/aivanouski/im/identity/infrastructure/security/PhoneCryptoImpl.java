package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.PhoneCrypto;
import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PhoneCryptoImpl implements PhoneCrypto {
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKeySpec hashKey;
    private final SecretKeySpec encKey;
    private final SecureRandom random = new SecureRandom();
    private final Base64.Encoder base64 = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();

    public PhoneCryptoImpl(PhoneCryptoProperties properties) {
        byte[] hashKeyBytes = decodeKey(properties.getHashKey(), "hashKey");
        byte[] encKeyBytes = decodeKey(properties.getEncKey(), "encKey");
        if (encKeyBytes.length != 16 && encKeyBytes.length != 24 && encKeyBytes.length != 32) {
            throw new ValidationException("encKey must be 16, 24, or 32 bytes.");
        }
        this.hashKey = new SecretKeySpec(hashKeyBytes, "HmacSHA256");
        this.encKey = new SecretKeySpec(encKeyBytes, "AES");
    }

    @Override
    public String hash(String phone) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hashKey);
            byte[] digest = mac.doFinal(phone.getBytes(StandardCharsets.UTF_8));
            return base64.encodeToString(digest);
        } catch (Exception ex) {
            throw new ValidationException("Phone hashing failed.");
        }
    }

    @Override
    public String encrypt(String phone) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(phone.getBytes(StandardCharsets.UTF_8));
            return base64.encodeToString(iv) + "." + base64.encodeToString(ciphertext);
        } catch (Exception ex) {
            throw new ValidationException("Phone encryption failed.");
        }
    }

    @Override
    public String decrypt(String encrypted) {
        try {
            String[] parts = encrypted.split("\\.", 2);
            if (parts.length != 2) {
                throw new ValidationException("Invalid encrypted phone value.");
            }
            byte[] iv = decoder.decode(parts[0]);
            byte[] ciphertext = decoder.decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Phone decryption failed.");
        }
    }

    private byte[] decodeKey(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Missing " + name + ".");
        }
        return decoder.decode(value);
    }
}
