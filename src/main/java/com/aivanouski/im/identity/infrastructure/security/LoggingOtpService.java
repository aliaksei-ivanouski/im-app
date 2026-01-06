package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.OtpCode;
import com.aivanouski.im.identity.application.auth.OtpService;
import com.aivanouski.im.identity.application.auth.TokenSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
@Component
public class LoggingOtpService implements OtpService {
    private static final Logger logger = LoggerFactory.getLogger(LoggingOtpService.class);
    private final SecureRandom random = new SecureRandom();
    private final TokenSettings tokenSettings;
    private final OtpProperties otpProperties;

    public LoggingOtpService(TokenSettings tokenSettings, OtpProperties otpProperties) {
        this.tokenSettings = tokenSettings;
        this.otpProperties = otpProperties;
    }

    @Override
    public OtpCode sendOtp(String phone) {
        String code = isTestPhone(phone)
                ? otpProperties.getTestCode()
                : String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plusSeconds(tokenSettings.otpTtlSeconds());
        logger.info("OTP for {} is {}", phone, code);
        return new OtpCode(code, expiresAt);
    }

    private boolean isTestPhone(String phone) {
        if (otpProperties.getTestPhones().contains(phone)) {
            return true;
        }
        return false;
    }

}
