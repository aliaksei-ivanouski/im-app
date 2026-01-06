package com.aivanouski.im.identity.presentation.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final UUID userId;
    private final UUID deviceId;
    private final String token;

    public JwtAuthenticationToken(UUID userId, UUID deviceId, String token, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.deviceId = deviceId;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }
}
