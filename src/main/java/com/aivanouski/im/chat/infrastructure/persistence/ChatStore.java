package com.aivanouski.im.chat.infrastructure.persistence;

import com.aivanouski.im.chat.application.ChatLookupRepository;
import com.aivanouski.im.chat.application.ChatRepository;
import java.time.Instant;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ChatStore implements ChatLookupRepository, ChatRepository {
    private final ChatJpaRepository chatJpaRepository;

    public ChatStore(ChatJpaRepository chatJpaRepository) {
        this.chatJpaRepository = chatJpaRepository;
    }

    @Override
    public boolean existsById(UUID id) {
        return chatJpaRepository.existsById(id);
    }

    @Override
    public UUID create(UUID createdBy) {
        ChatEntity entity = new ChatEntity(UUID.randomUUID(), Instant.now());
        chatJpaRepository.save(entity);
        return entity.getId();
    }
}
