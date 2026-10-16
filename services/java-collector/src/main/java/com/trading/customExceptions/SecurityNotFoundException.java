package com.trading.customExceptions;

public class SecurityNotFoundException extends RuntimeException {
    public SecurityNotFoundException(String message) {
        super(message + " not found in the database.");
    }
    
}
