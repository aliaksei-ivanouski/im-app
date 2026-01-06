package com.aivanouski.im.identity.infrastructure.persistence;

import com.aivanouski.im.identity.application.auth.UserRepository;
import com.aivanouski.im.identity.application.user.UserLookupRepository;
import com.aivanouski.im.identity.domain.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserStore implements UserLookupRepository, UserRepository {
    private final UserJpaRepository userJpaRepository;

    public UserStore(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public boolean existsById(UUID id) {
        return userJpaRepository.existsById(id);
    }

    @Override
    public Optional<UserAccount> findByPhoneHash(String phoneHash) {
        return userJpaRepository.findByPhoneHash(phoneHash).map(this::toDomain);
    }

    @Override
    public UserAccount save(UserAccount user) {
        UserEntity saved = userJpaRepository.save(toEntity(user));
        return toDomain(saved);
    }

    private UserAccount toDomain(UserEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getPhoneHash(),
                entity.getPhoneEncrypted(),
                entity.getCreatedAt()
        );
    }

    private UserEntity toEntity(UserAccount user) {
        return new UserEntity(user.id(), user.phoneHash(), user.phoneEncrypted(), user.createdAt());
    }
}
