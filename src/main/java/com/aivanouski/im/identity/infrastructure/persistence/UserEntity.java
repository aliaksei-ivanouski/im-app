package com.aivanouski.im.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserEntity {
    @Id
    private UUID id;

    @Column(name = "phone_hash", nullable = false, unique = true)
    private String phoneHash;

    @Column(name = "phone_encrypted", nullable = false)
    private String phoneEncrypted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEntity() {
    }

    public UserEntity(UUID id, String phoneHash, String phoneEncrypted, Instant createdAt) {
        this.id = id;
        this.phoneHash = phoneHash;
        this.phoneEncrypted = phoneEncrypted;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getPhoneHash() {
        return phoneHash;
    }

    public String getPhoneEncrypted() {
        return phoneEncrypted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
