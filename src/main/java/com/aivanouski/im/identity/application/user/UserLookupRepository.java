package com.aivanouski.im.identity.application.user;

import java.util.UUID;

public interface UserLookupRepository {
    boolean existsById(UUID id);
}
