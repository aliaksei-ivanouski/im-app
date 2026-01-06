package com.aivanouski.im.shared.exception;

public class OptimisticLockingException extends ApplicationException {
    public OptimisticLockingException(String message) {
        super("optimistic_lock_failed", message);
    }
}
