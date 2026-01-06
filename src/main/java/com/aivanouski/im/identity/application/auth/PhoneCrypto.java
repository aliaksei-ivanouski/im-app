package com.aivanouski.im.identity.application.auth;

public interface PhoneCrypto {
    String hash(String phone);

    String encrypt(String phone);

    String decrypt(String encrypted);
}
