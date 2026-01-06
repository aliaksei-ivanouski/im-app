package com.aivanouski.im.shared.error;

import com.aivanouski.im.shared.exception.NotFoundException;
import com.aivanouski.im.shared.exception.OptimisticLockingException;
import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WsErrorMapperTest {
    private final WsErrorMapper mapper = new WsErrorMapper();

    @Test
    void mapsApplicationExceptionToWsError() {
        var response = mapper.toResponse("req-1", new ValidationException("bad"));

        assertEquals("error", response.type());
        assertEquals("req-1", response.requestId());
        assertEquals("validation_error", response.error().code());
        assertEquals("bad", response.error().message());
    }

    @Test
    void unwrapsCauseWhenPresent() {
        var response = mapper.toResponse("req-2", new RuntimeException(new NotFoundException("missing")));

        assertEquals("not_found", response.error().code());
        assertEquals("missing", response.error().message());
    }

    @Test
    void mapsOptimisticLockingExceptionToWsError() {
        var response = mapper.toResponse("req-4", new OptimisticLockingException("conflict"));

        assertEquals("optimistic_lock_failed", response.error().code());
        assertEquals("conflict", response.error().message());
    }

    @Test
    void mapsUnknownExceptionToInternalError() {
        var response = mapper.toResponse("req-3", new RuntimeException("boom"));

        assertEquals("internal_error", response.error().code());
        assertEquals("Unexpected server error.", response.error().message());
    }
}
