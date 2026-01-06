package com.aivanouski.im.message.application;

import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MessageCreateValidator {
    public void validate(UUID userId, UUID chatId, String payload) {
        if (userId == null || chatId == null) {
            throw new ValidationException("userId and chatId are required.");
        }
        if (payload == null || payload.trim().isEmpty()) {
            throw new ValidationException("payload must not be blank.");
        }
    }
}
