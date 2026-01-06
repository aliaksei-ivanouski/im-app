package com.aivanouski.im.message.application;

import com.aivanouski.im.message.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MessageLookupRepository {
    Page<Message> findByChat(UUID chatId, Pageable pageable);

    Page<Message> findByChatAndUser(UUID chatId, UUID userId, Pageable pageable);
}
