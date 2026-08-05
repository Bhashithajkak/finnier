package com.example.finnier.exception;

public class TokenValidationException extends JwtServiceException{
    public TokenValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenValidationException(String message){
        super(message);
    }
}
