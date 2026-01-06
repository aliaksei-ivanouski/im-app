package com.aivanouski.im.identity.application.auth;

public interface OtpHasher {
    String hash(String otp);

    boolean matches(String otp, String hash);
}
