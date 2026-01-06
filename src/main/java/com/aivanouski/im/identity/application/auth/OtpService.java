package com.aivanouski.im.identity.application.auth;

public interface OtpService {
    OtpCode sendOtp(String phone);
}
