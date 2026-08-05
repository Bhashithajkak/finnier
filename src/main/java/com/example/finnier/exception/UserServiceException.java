package com.example.finnier.exception;

public abstract class UserServiceException extends RuntimeException {
    public UserServiceException(String message) {
        super(message);
    }
}
