package com.aivanouski.im.identity.application.auth;

public interface TokenSettings {
    long accessTtlSeconds();

    long refreshTtlSeconds();

    long deviceChallengeTtlSeconds();

    long otpTtlSeconds();
}
