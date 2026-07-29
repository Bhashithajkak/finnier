package com.example.finnier.exception;

public class InvalidCredentialsException extends UserServiceException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
