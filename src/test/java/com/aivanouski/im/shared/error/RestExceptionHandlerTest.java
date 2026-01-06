package com.aivanouski.im.shared.error;

import com.aivanouski.im.shared.exception.ApplicationException;
import com.aivanouski.im.shared.exception.NotFoundException;
import com.aivanouski.im.shared.exception.OptimisticLockingException;
import com.aivanouski.im.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestExceptionHandlerTest {
    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void mapsValidationExceptionToBadRequest() {
        ResponseEntity<RestErrorResponse> response = handler.handleApplication(new ValidationException("bad"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("validation_error", response.getBody().code());
    }

    @Test
    void mapsNotFoundExceptionToNotFound() {
        ResponseEntity<RestErrorResponse> response = handler.handleApplication(new NotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("not_found", response.getBody().code());
    }

    @Test
    void mapsOptimisticLockingExceptionToConflict() {
        ResponseEntity<RestErrorResponse> response = handler.handleApplication(new OptimisticLockingException("conflict"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("optimistic_lock_failed", response.getBody().code());
    }

    @Test
    void mapsUnknownApplicationExceptionToBadRequest() {
        ApplicationException ex = new ApplicationException("custom", "custom message");
        ResponseEntity<RestErrorResponse> response = handler.handleApplication(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("custom", response.getBody().code());
        assertEquals("custom message", response.getBody().message());
    }

    @Test
    void mapsUnexpectedExceptionToInternalError() {
        ResponseEntity<RestErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("internal_error", response.getBody().code());
        assertEquals("Unexpected server error.", response.getBody().message());
    }
}
