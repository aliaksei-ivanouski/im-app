package com.aivanouski.im.message.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID> {
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(cast(:chatId as text)))", nativeQuery = true)
    void lockChat(@Param("chatId") UUID chatId);

    @Query(value = "SELECT COALESCE(MAX(message_chat_n), 0) + 1 FROM message WHERE chat_id = :chatId", nativeQuery = true)
    Integer nextMessageNumber(@Param("chatId") UUID chatId);

    @Modifying
    @Query("UPDATE MessageEntity m SET m.payload = :payload, m.version = m.version + 1 WHERE m.id = :id AND m.userId = :userId AND m.chatId = :chatId AND m.version = :version")
    int updatePayload(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("chatId") UUID chatId,
            @Param("version") int version,
            @Param("payload") String payload
    );

    Page<MessageEntity> findByChatId(UUID chatId, Pageable pageable);

    Page<MessageEntity> findByChatIdAndUserId(UUID chatId, UUID userId, Pageable pageable);
}
