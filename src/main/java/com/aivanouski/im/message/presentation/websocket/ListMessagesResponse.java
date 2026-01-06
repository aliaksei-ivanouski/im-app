package com.aivanouski.im.message.presentation.websocket;

import java.util.List;

public record ListMessagesResponse(
        List<MessageDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
