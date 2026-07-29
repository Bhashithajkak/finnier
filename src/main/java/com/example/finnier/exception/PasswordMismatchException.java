package com.example.finnier.exception;

public class PasswordMismatchException extends UserServiceException {
    public PasswordMismatchException(String message) {
        super(message);
    }
}
