package com.aivanouski.im.message.application;

import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageListValidatorTest {
    private final MessageListValidator validator = new MessageListValidator();

    @Test
    void acceptsValidPayload() {
        validator.validate(UUID.randomUUID(), 0, 20);
    }

    @Test
    void rejectsMissingChatId() {
        assertThrows(ValidationException.class, () -> validator.validate(null, 0, 20));
    }

    @Test
    void rejectsInvalidPage() {
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), -1, 20));
    }

    @Test
    void rejectsInvalidSize() {
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), 0, 0));
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), 0, 25));
    }
}
