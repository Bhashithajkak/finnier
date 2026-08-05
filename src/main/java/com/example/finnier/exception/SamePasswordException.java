package com.example.finnier.exception;

public class SamePasswordException extends UserServiceException {
    public SamePasswordException(String message) {
        super(message);
    }
}
