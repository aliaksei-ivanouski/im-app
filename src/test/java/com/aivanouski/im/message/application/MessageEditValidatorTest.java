package com.aivanouski.im.message.application;

import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageEditValidatorTest {
    private final MessageEditValidator validator = new MessageEditValidator();

    @Test
    void acceptsValidPayload() {
        validator.validate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, "edit");
    }

    @Test
    void rejectsMissingMessageId() {
        assertThrows(ValidationException.class, () -> validator.validate(null, UUID.randomUUID(), UUID.randomUUID(), 0, "edit"));
    }

    @Test
    void rejectsMissingUserOrChat() {
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), null, UUID.randomUUID(), 0, "edit"));
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), UUID.randomUUID(), null, 0, "edit"));
    }

    @Test
    void rejectsNegativeVersion() {
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), -1, "edit"));
    }

    @Test
    void rejectsBlankPayload() {
        assertThrows(ValidationException.class, () -> validator.validate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, " "));
    }
}
