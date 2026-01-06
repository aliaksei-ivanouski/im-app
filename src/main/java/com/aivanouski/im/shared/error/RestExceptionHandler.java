package com.aivanouski.im.shared.error;

import com.aivanouski.im.shared.exception.ApplicationException;
import com.aivanouski.im.shared.exception.NotFoundException;
import com.aivanouski.im.shared.exception.OptimisticLockingException;
import com.aivanouski.im.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<RestErrorResponse> handleApplication(ApplicationException ex) {
        HttpStatus status = switch (ex) {
            case ValidationException ignored -> HttpStatus.BAD_REQUEST;
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case OptimisticLockingException ignored -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new RestErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled REST error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RestErrorResponse("internal_error", "Unexpected server error."));
    }
}
