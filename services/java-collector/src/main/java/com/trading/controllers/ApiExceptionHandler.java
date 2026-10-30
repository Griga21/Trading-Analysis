package com.trading.controllers;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.trading.customExceptions.SecuritiesAlreadyExistsException;
import com.trading.customExceptions.SecurityNotFoundException;
import com.trading.dto.ApiErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SecurityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            SecurityNotFoundException exception,
            WebRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(SecuritiesAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            SecuritiesAlreadyExistsException exception,
            WebRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception exception,
            WebRequest request) {
        String message;
        if (exception instanceof MethodArgumentNotValidException validationException) {
            message = validationException.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        } else {
            message = exception.getMessage();
        }
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({WebClientResponseException.class, WebClientRequestException.class})
    public ResponseEntity<ApiErrorResponse> handleMoexFailure(
            Exception exception,
            WebRequest request) {
        return error(
                HttpStatus.BAD_GATEWAY,
                "MOEX request failed. Please try again later.",
                request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String message,
            WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path));
    }
}
