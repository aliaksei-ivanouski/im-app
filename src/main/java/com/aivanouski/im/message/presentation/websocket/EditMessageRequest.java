package com.aivanouski.im.message.presentation.websocket;

import java.util.UUID;

public record EditMessageRequest(
        UUID messageId,
        UUID userId,
        UUID chatId,
        Integer version,
        String payload
) {
}
