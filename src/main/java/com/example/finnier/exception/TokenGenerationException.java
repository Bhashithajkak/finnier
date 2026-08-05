package com.example.finnier.exception;

public class TokenGenerationException extends JwtServiceException{

    public TokenGenerationException(String message) {
        super(message);
    }

    public TokenGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
