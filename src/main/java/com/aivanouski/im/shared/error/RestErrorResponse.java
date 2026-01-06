package com.aivanouski.im.shared.error;

public record RestErrorResponse(
        String code,
        String message
) {
}
