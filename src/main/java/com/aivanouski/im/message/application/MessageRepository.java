package com.aivanouski.im.message.application;

import com.aivanouski.im.message.domain.Message;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    Message create(UUID userId, UUID chatId, String payload);

    Optional<Message> updatePayload(UUID messageId, UUID userId, UUID chatId, int version, String payload);
}
