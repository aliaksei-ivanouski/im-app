package com.aivanouski.im.message.presentation.websocket;

import com.aivanouski.im.message.domain.Message;

import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID userId,
        UUID chatId,
        int messageChatN,
        int version,
        String payload
) {
    public static MessageDto from(Message message) {
        return new MessageDto(
                message.id(),
                message.userId(),
                message.chatId(),
                message.messageChatN(),
                message.version(),
                message.payload()
        );
    }
}
