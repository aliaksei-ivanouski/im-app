package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.SignatureVerifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class SignatureVerifierImpl implements SignatureVerifier {
    private static final Base64.Decoder BASE64 = Base64.getDecoder();

    public boolean verify(String keyAlg, String publicKeyBase64, String message, String signatureBase64) {
        try {
            PublicKey publicKey = decodePublicKey(keyAlg, publicKeyBase64);
            Signature signature = Signature.getInstance(signatureAlgorithm(keyAlg));
            signature.initVerify(publicKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(BASE64.decode(signatureBase64));
        } catch (Exception ex) {
            return false;
        }
    }

    private PublicKey decodePublicKey(String keyAlg, String publicKeyBase64) throws Exception {
        byte[] decoded = BASE64.decode(publicKeyBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance(keyAlg).generatePublic(spec);
    }

    private String signatureAlgorithm(String keyAlg) {
        return switch (keyAlg) {
            case "EC" -> "SHA256withECDSA";
            case "RSA" -> "SHA256withRSA";
            default -> throw new IllegalArgumentException("Unsupported key algorithm: " + keyAlg);
        };
    }
}
