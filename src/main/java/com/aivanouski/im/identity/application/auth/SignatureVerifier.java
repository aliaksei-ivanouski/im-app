package com.aivanouski.im.identity.application.auth;

public interface SignatureVerifier {
    boolean verify(String keyAlg, String publicKeyBase64, String message, String signatureBase64);
}
