package com.aivanouski.im.message.presentation.websocket;

import java.util.UUID;

public record ListMessagesRequest(
        UUID chatId,
        UUID userId,
        Integer page,
        Integer size
) {
}
