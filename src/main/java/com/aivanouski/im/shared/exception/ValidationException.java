package com.aivanouski.im.shared.exception;

public class ValidationException extends ApplicationException {
    public ValidationException(String message) {
        super("validation_error", message);
    }
}
