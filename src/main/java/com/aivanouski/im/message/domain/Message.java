package com.aivanouski.im.message.domain;

import java.util.UUID;

public record Message(
        UUID id,
        UUID userId,
        UUID chatId,
        int messageChatN,
        int version,
        String payload
) {
}
