package com.aivanouski.im.identity.domain.auth;

public record AccessToken(String value, long expiresInSeconds) {
}
