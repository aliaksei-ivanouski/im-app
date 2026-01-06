package com.aivanouski.im.message.presentation.websocket;

import java.util.UUID;

public record CreateMessageRequest(
        UUID userId,
        UUID chatId,
        String payload
) {
}
