package com.aivanouski.im.shared.error;

import com.aivanouski.im.shared.exception.ApplicationException;
import com.aivanouski.im.message.presentation.websocket.WsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WsErrorMapper {
    private static final Logger log = LoggerFactory.getLogger(WsErrorMapper.class);

    public WsResponse toResponse(String requestId, Throwable error) {
        Throwable root = error.getCause() == null ? error : error.getCause();
        if (root instanceof ApplicationException appEx) {
            return WsResponse.error(requestId, appEx.getCode(), appEx.getMessage());
        }
        log.error("Unhandled websocket error", error);
        return WsResponse.error(requestId, "internal_error", "Unexpected server error.");
    }
}
