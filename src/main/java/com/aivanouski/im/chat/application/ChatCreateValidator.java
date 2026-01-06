package com.aivanouski.im.chat.application;

import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChatCreateValidator {
    public void validate(UUID userId) {
        if (userId == null) {
            throw new ValidationException("userId is required.");
        }
    }
}
