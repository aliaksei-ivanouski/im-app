package com.aivanouski.im.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device")
public class DeviceEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "public_key")
    private String publicKey;

    @Column(name = "public_key_alg")
    private String publicKeyAlg;

    @Column(name = "challenge")
    private String challenge;

    @Column(name = "challenge_expires_at")
    private Instant challengeExpiresAt;


    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    protected DeviceEntity() {
    }

    public DeviceEntity(UUID id, UUID userId, String publicKey, String publicKeyAlg, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.publicKey = publicKey;
        this.publicKeyAlg = publicKeyAlg;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyAlg() {
        return publicKeyAlg;
    }

    public String getChallenge() {
        return challenge;
    }

    public Instant getChallengeExpiresAt() {
        return challengeExpiresAt;
    }


    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setChallenge(String challenge, Instant expiresAt) {
        this.challenge = challenge;
        this.challengeExpiresAt = expiresAt;
    }


    public void setVerified(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public void setPublicKey(String publicKey, String publicKeyAlg) {
        this.publicKey = publicKey;
        this.publicKeyAlg = publicKeyAlg;
    }
}
