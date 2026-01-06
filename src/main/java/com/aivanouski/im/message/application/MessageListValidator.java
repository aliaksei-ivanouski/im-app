package com.aivanouski.im.message.application;

import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MessageListValidator {
    private static final int MAX_PAGE_SIZE = 20;

    public void validate(UUID chatId, int page, int size) {
        if (chatId == null) {
            throw new ValidationException("chatId is required.");
        }
        if (page < 0) {
            throw new ValidationException("page must be zero or positive.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException("size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
    }
}
