package com.aivanouski.im.message.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "message")
public class MessageEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(name = "message_chat_n", nullable = false)
    private int messageChatN;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "payload", nullable = false)
    private String payload;

    protected MessageEntity() {
    }

    public MessageEntity(UUID id, UUID userId, UUID chatId, int messageChatN, int version, String payload) {
        this.id = id;
        this.userId = userId;
        this.chatId = chatId;
        this.messageChatN = messageChatN;
        this.version = version;
        this.payload = payload;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getChatId() {
        return chatId;
    }

    public int getMessageChatN() {
        return messageChatN;
    }

    public int getVersion() {
        return version;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
