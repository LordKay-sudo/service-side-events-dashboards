package com.logistics.dashboard.service;

public class OptimisticLockingFailureException extends RuntimeException {

    public OptimisticLockingFailureException(String message) {
        super(message);
    }
}
