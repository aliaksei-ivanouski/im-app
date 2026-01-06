package com.aivanouski.im.identity.presentation.rest;

import java.util.UUID;

public record AuthStartRequest(
        String phone,
        UUID deviceId
) {
}
