package com.aivanouski.im.message.application;

import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MessageEditValidator {
    public void validate(UUID messageId, UUID userId, UUID chatId, int version, String payload) {
        if (messageId == null) {
            throw new ValidationException("messageId is required.");
        }
        if (userId == null || chatId == null) {
            throw new ValidationException("userId and chatId are required.");
        }
        if (version < 0) {
            throw new ValidationException("version must be non-negative.");
        }
        if (payload == null || payload.trim().isEmpty()) {
            throw new ValidationException("payload must not be blank.");
        }
    }
}
