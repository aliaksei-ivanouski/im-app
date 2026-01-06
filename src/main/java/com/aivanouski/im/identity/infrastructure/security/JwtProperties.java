package com.aivanouski.im.identity.infrastructure.security;

import com.aivanouski.im.identity.application.auth.TokenSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties implements TokenSettings {
    private String secret;
    private String issuer;
    private long accessTtlSeconds = 900;
    private long refreshTtlSeconds = 2592000;
    private long deviceChallengeTtlSeconds = 300;
    private long otpTtlSeconds = 300;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public void setAccessTtlSeconds(long accessTtlSeconds) {
        this.accessTtlSeconds = accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public void setRefreshTtlSeconds(long refreshTtlSeconds) {
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public long getDeviceChallengeTtlSeconds() {
        return deviceChallengeTtlSeconds;
    }

    public void setDeviceChallengeTtlSeconds(long deviceChallengeTtlSeconds) {
        this.deviceChallengeTtlSeconds = deviceChallengeTtlSeconds;
    }

    public long getOtpTtlSeconds() {
        return otpTtlSeconds;
    }

    public void setOtpTtlSeconds(long otpTtlSeconds) {
        this.otpTtlSeconds = otpTtlSeconds;
    }

    @Override
    public long accessTtlSeconds() {
        return accessTtlSeconds;
    }

    @Override
    public long refreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    @Override
    public long deviceChallengeTtlSeconds() {
        return deviceChallengeTtlSeconds;
    }

    @Override
    public long otpTtlSeconds() {
        return otpTtlSeconds;
    }
}
