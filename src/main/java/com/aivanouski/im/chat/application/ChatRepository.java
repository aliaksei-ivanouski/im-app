package com.aivanouski.im.chat.application;

import java.util.UUID;

public interface ChatRepository {
    UUID create(UUID createdBy);
}
