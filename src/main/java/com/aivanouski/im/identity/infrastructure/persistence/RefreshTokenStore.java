package com.aivanouski.im.identity.infrastructure.persistence;

import com.aivanouski.im.identity.application.auth.RefreshTokenLookupRepository;
import com.aivanouski.im.identity.application.auth.RefreshTokenRepository;
import com.aivanouski.im.identity.domain.RefreshTokenRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RefreshTokenStore implements RefreshTokenRepository, RefreshTokenLookupRepository {
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public RefreshTokenStore(RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public RefreshTokenRecord save(RefreshTokenRecord token) {
        RefreshTokenEntity saved = refreshTokenJpaRepository.save(toEntity(token));
        return toDomain(saved);
    }

    private RefreshTokenRecord toDomain(RefreshTokenEntity entity) {
        return new RefreshTokenRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getDeviceId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt()
        );
    }

    private RefreshTokenEntity toEntity(RefreshTokenRecord token) {
        RefreshTokenEntity entity = new RefreshTokenEntity(
                token.id(),
                token.userId(),
                token.deviceId(),
                token.tokenHash(),
                token.expiresAt(),
                token.createdAt()
        );
        if (token.revokedAt() != null) {
            entity.revoke(token.revokedAt());
        }
        return entity;
    }
}
