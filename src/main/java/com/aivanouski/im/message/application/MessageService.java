package com.aivanouski.im.message.application;

import com.aivanouski.im.shared.exception.OptimisticLockingException;
import com.aivanouski.im.chat.application.ChatLookupRepository;
import com.aivanouski.im.identity.application.user.UserLookupRepository;
import com.aivanouski.im.message.domain.Message;
import com.aivanouski.im.shared.exception.ValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageLookupRepository messageLookupRepository;
    private final UserLookupRepository userLookupRepository;
    private final ChatLookupRepository chatLookupRepository;
    private final MessageCreateValidator messageCreateValidator;
    private final MessageEditValidator messageEditValidator;
    private final MessageListValidator messageListValidator;

    public MessageService(
            MessageRepository messageRepository,
            MessageLookupRepository messageLookupRepository,
            UserLookupRepository userLookupRepository,
            ChatLookupRepository chatLookupRepository,
            MessageCreateValidator messageCreateValidator,
            MessageEditValidator messageEditValidator,
            MessageListValidator messageListValidator
    ) {
        this.messageRepository = messageRepository;
        this.messageLookupRepository = messageLookupRepository;
        this.userLookupRepository = userLookupRepository;
        this.chatLookupRepository = chatLookupRepository;
        this.messageCreateValidator = messageCreateValidator;
        this.messageEditValidator = messageEditValidator;
        this.messageListValidator = messageListValidator;
    }

    @Transactional
    public Message createMessage(UUID userId, UUID chatId, String payload) {
        messageCreateValidator.validate(userId, chatId, payload);
        ensureUserExists(userId);
        ensureChatExists(chatId);
        return messageRepository.create(userId, chatId, payload.trim());
    }

    @Transactional
    public Message editMessage(UUID messageId, UUID userId, UUID chatId, int version, String payload) {
        messageEditValidator.validate(messageId, userId, chatId, version, payload);
        ensureUserExists(userId);
        ensureChatExists(chatId);
        return messageRepository.updatePayload(messageId, userId, chatId, version, payload.trim())
                .orElseThrow(() -> new OptimisticLockingException("Message version mismatch or message not found."));
    }

    @Transactional(readOnly = true)
    public Page<Message> listMessages(UUID chatId, UUID userId, int page, int size) {
        messageListValidator.validate(chatId, page, size);
        ensureChatExists(chatId);
        if (userId != null) {
            ensureUserExists(userId);
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("messageChatN").ascending());
        if (userId == null) {
            return messageLookupRepository.findByChat(chatId, pageable);
        }
        return messageLookupRepository.findByChatAndUser(chatId, userId, pageable);
    }

    private void ensureUserExists(UUID userId) {
        if (!userLookupRepository.existsById(userId)) {
            throw new ValidationException("userId does not exist.");
        }
    }

    private void ensureChatExists(UUID chatId) {
        if (!chatLookupRepository.existsById(chatId)) {
            throw new ValidationException("chatId does not exist.");
        }
    }
}
