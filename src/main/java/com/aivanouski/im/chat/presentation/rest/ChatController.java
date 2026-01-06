package com.aivanouski.im.chat.presentation.rest;

import com.aivanouski.im.chat.application.ChatService;
import com.aivanouski.im.shared.exception.ValidationException;
import com.aivanouski.im.identity.presentation.security.JwtAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(path = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatResponse> createChat(org.springframework.security.core.Authentication authentication) {
        return Mono.fromCallable(() -> {
                    if (!(authentication instanceof JwtAuthenticationToken token)) {
                        throw new ValidationException("Invalid authentication.");
                    }
                    return chatService.createChat((java.util.UUID) token.getPrincipal());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(ChatResponse::new);
    }
}
