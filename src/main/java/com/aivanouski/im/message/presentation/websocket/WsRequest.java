package com.aivanouski.im.message.presentation.websocket;

import com.fasterxml.jackson.databind.JsonNode;

public record WsRequest(
        String type,
        String requestId,
        JsonNode payload
) {
}
