package com.aivanouski.im.chat.presentation.rest;

import com.aivanouski.im.chat.application.ChatService;
import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import reactor.test.StepVerifier;

class ChatControllerTest {
    @Test
    void rejectsNonJwtAuthentication() {
        ChatService chatService = Mockito.mock(ChatService.class);
        ChatController controller = new ChatController(chatService);
        Authentication authentication = Mockito.mock(Authentication.class);

        StepVerifier.create(controller.createChat(authentication))
                .expectErrorMatches(ex -> ex instanceof ValidationException
                        && "Invalid authentication.".equals(ex.getMessage()))
                .verify();

        Mockito.verifyNoInteractions(chatService);
    }
}
