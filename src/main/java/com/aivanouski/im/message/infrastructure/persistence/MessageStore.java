package com.aivanouski.im.message.infrastructure.persistence;

import com.aivanouski.im.message.application.MessageLookupRepository;
import com.aivanouski.im.message.application.MessageRepository;
import com.aivanouski.im.message.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MessageStore implements MessageRepository, MessageLookupRepository {
    private final MessageJpaRepository messageJpaRepository;

    public MessageStore(MessageJpaRepository messageJpaRepository) {
        this.messageJpaRepository = messageJpaRepository;
    }

    @Override
    @Transactional
    public Message create(UUID userId, UUID chatId, String payload) {
        messageJpaRepository.lockChat(chatId);
        int nextNumber = messageJpaRepository.nextMessageNumber(chatId);
        MessageEntity entity = new MessageEntity(UUID.randomUUID(), userId, chatId, nextNumber, 0, payload);
        return toDomain(messageJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public Optional<Message> updatePayload(UUID messageId, UUID userId, UUID chatId, int version, String payload) {
        int updated = messageJpaRepository.updatePayload(messageId, userId, chatId, version, payload);
        if (updated == 0) {
            return Optional.empty();
        }
        return messageJpaRepository.findById(messageId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> findByChat(UUID chatId, Pageable pageable) {
        return messageJpaRepository.findByChatId(chatId, pageable).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> findByChatAndUser(UUID chatId, UUID userId, Pageable pageable) {
        return messageJpaRepository.findByChatIdAndUserId(chatId, userId, pageable).map(this::toDomain);
    }

    private Message toDomain(MessageEntity entity) {
        return new Message(
                entity.getId(),
                entity.getUserId(),
                entity.getChatId(),
                entity.getMessageChatN(),
                entity.getVersion(),
                entity.getPayload()
        );
    }
}
