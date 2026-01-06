package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.identity.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<UserAccount> findByPhoneHash(String phoneHash);

    UserAccount save(UserAccount user);

    boolean existsById(UUID id);
}
