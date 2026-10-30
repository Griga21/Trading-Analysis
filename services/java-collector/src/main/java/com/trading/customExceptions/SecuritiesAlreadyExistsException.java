package com.trading.customExceptions;

public class SecuritiesAlreadyExistsException extends RuntimeException {

    public SecuritiesAlreadyExistsException(String message) {
        super(message + " already exists in the database.");
    }
}