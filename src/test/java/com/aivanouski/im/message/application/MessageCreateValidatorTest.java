package com.aivanouski.im.message.application;

import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageCreateValidatorTest {
    private final MessageCreateValidator validator = new MessageCreateValidator();

    @Test
    void acceptsValidPayload() {
        validator.validate(UUID.randomUUID(), UUID.randomUUID(), "hello");
    }

    @Test
    void rejectsMissingIds() {
        assertThrows(ValidationException.class, () -> validator.validate(null, UUID.randomUUID(), "hello"));
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), null, "hello"));
    }

    @Test
    void rejectsBlankPayload() {
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), UUID.randomUUID(), " "));
    }
}
