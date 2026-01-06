package com.aivanouski.im.chat.application;

import com.aivanouski.im.shared.exception.ValidationException;
import com.aivanouski.im.identity.application.user.UserLookupRepository;
import org.mockito.Mockito;
import com.aivanouski.im.chat.application.ChatRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatCreateValidatorTest {
    private final ChatCreateValidator validator = new ChatCreateValidator();

    @Test
    void acceptsValidPayload() {
        validator.validate(UUID.randomUUID());
    }

    @Test
    void rejectsMissingUserId() {
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }

    @Test
    void createChatRejectsMissingUser() {
        UserLookupRepository userLookupRepository = Mockito.mock(UserLookupRepository.class);
        ChatRepository chatRepository = Mockito.mock(ChatRepository.class);
        ChatService chatService = new ChatService(chatRepository, userLookupRepository, validator);
        java.util.UUID userId = java.util.UUID.randomUUID();

        Mockito.when(userLookupRepository.existsById(userId)).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class, () -> chatService.createChat(userId));
        assertEquals("userId does not exist.", ex.getMessage());
    }
}
