package com.aivanouski.im.chat.application;

import com.aivanouski.im.identity.application.user.UserLookupRepository;
import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ChatService {
    private final ChatRepository chatRepository;
    private final UserLookupRepository userLookupRepository;
    private final ChatCreateValidator chatCreateValidator;

    public ChatService(
            ChatRepository chatRepository,
            UserLookupRepository userLookupRepository,
            ChatCreateValidator chatCreateValidator
    ) {
        this.chatRepository = chatRepository;
        this.userLookupRepository = userLookupRepository;
        this.chatCreateValidator = chatCreateValidator;
    }

    @Transactional
    public UUID createChat(UUID userId) {
        chatCreateValidator.validate(userId);
        if (!userLookupRepository.existsById(userId)) {
            throw new ValidationException("userId does not exist.");
        }
        return chatRepository.create(userId);
    }
}
