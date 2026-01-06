package com.aivanouski.im.identity.presentation.rest;

import java.util.UUID;

public record RefreshRequest(
        UUID deviceId,
        String refreshToken
) {
}
