package com.aivanouski.im.chat.application;

import java.util.UUID;

public interface ChatLookupRepository {
    boolean existsById(UUID id);
}
