package com.aivanouski.im.shared.exception;

public class NotFoundException extends ApplicationException {
    public NotFoundException(String message) {
        super("not_found", message);
    }
}
