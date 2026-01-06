package com.aivanouski.im.identity.application.auth;

public interface RefreshTokenHasher {
    String hash(String token);
}
