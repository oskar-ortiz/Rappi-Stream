package com.rappistream.orderapi.infrastructure.web;

import com.rappistream.orderapi.application.BrokerUnavailableException;
import com.rappistream.orderapi.application.CreateOrderService;
import com.rappistream.orderapi.domain.CreateOrderRequest;
import com.rappistream.orderapi.domain.ErrorDetail;
import com.rappistream.orderapi.domain.ErrorResponse;
import com.rappistream.orderapi.domain.OrderAcceptedResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.add(new ErrorDetail(normalizeField(fe.getField()), fe.getDefaultMessage()));
        }
        if (details.isEmpty()) {
            details.add(new ErrorDetail("$", "validation failed"));
        }
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_ERROR", "Request validation failed", details, Instant.now()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        List<ErrorDetail> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(v -> details.add(new ErrorDetail(
                v.getPropertyPath().toString(), v.getMessage())));
        if (details.isEmpty()) {
            details.add(new ErrorDetail("$", "validation failed"));
        }
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_ERROR", "Request validation failed", details, Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        String message = cause != null ? cause.getMessage() : ex.getMessage();
        boolean malformedJson = message == null
                || message.toLowerCase().contains("json")
                || message.toLowerCase().contains("parse");
        if (malformedJson && (cause == null || cause instanceof com.fasterxml.jackson.core.JsonProcessingException)) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "INVALID_JSON", "Request body contains malformed JSON",
                    List.of(new ErrorDetail("$", "malformed JSON")), Instant.now()));
        }
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_ERROR", "Request validation failed",
                List.of(new ErrorDetail("$", message == null ? "invalid request" : message)), Instant.now()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new ErrorResponse(
                "UNSUPPORTED_MEDIA_TYPE", "Content-Type must be application/json",
                List.of(), Instant.now()));
    }

    @ExceptionHandler(BrokerUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleBroker(BrokerUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                "BROKER_UNAVAILABLE",
                "The order could not be accepted because the message broker is unavailable",
                List.of(), Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                "INTERNAL_ERROR", "Internal server error", List.of(), Instant.now()));
    }

    private String normalizeField(String field) {
        return field == null || field.isBlank() ? "$" : field;
    }
}
