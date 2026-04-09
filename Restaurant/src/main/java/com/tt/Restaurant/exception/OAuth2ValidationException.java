package com.tt.Restaurant.exception;

public class OAuth2ValidationException extends RuntimeException {
    private final String errorCode;

    public OAuth2ValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}