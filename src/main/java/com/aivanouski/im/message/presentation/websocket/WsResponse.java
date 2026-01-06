package com.aivanouski.im.message.presentation.websocket;

import com.aivanouski.im.shared.error.ErrorInfo;

public record WsResponse(
        String type,
        String requestId,
        String status,
        Object payload,
        ErrorInfo error
) {
    public static WsResponse ok(String type, String requestId, Object payload) {
        return new WsResponse(type, requestId, "ok", payload, null);
    }

    public static WsResponse error(String requestId, String code, String message) {
        return new WsResponse("error", requestId, "error", null, new ErrorInfo(code, message));
    }
}
