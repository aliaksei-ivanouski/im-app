package com.aivanouski.im.identity.application.auth;

import com.aivanouski.im.identity.domain.DeviceRecord;

public interface DeviceRepository {
    DeviceRecord save(DeviceRecord device);
}
