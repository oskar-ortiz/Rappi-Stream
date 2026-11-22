package com.rappistream.orderapi.infrastructure.web;

import com.rappistream.orderapi.application.BrokerUnavailableException;
import com.rappistream.orderapi.application.CreateOrderService;
import com.rappistream.orderapi.domain.CreateOrderRequest;
import com.rappistream.orderapi.domain.ErrorDetail;
import com.rappistream.orderapi.domain.ErrorResponse;
import com.rappistream.orderapi.domain.InvalidOrderRequestException;
import com.rappistream.orderapi.domain.OrderAcceptedResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
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
        InvalidOrderRequestException invalidField = findInvalidOrderField(ex);
        if (invalidField != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "VALIDATION_ERROR", "Request validation failed",
                    List.of(new ErrorDetail(invalidField.field(), invalidField.reason())), Instant.now()));
        }
        String mismatchField = findMismatchField(ex);
        if (mismatchField != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "VALIDATION_ERROR", "Request validation failed",
                    List.of(new ErrorDetail(mismatchField, "value has an invalid type or format")),
                    Instant.now()));
        }
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "INVALID_JSON", "Request body is missing or is not valid JSON",
                List.of(new ErrorDetail("$", "malformed or empty JSON input")), Instant.now()));
    }

    private InvalidOrderRequestException findInvalidOrderField(Throwable ex) {
        Throwable current = ex;
        for (int depth = 0; depth < 5 && current != null; depth++) {
            if (current instanceof InvalidOrderRequestException invalid) {
                return invalid;
            }
            current = current.getCause();
        }
        return null;
    }

    private String findMismatchField(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            StringBuilder path = new StringBuilder();
            for (JsonMappingException.Reference reference : mapping.getPath()) {
                if (reference.getFieldName() != null) {
                    if (path.length() > 0) {
                        path.append('.');
                    }
                    path.append(reference.getFieldName());
                } else if (reference.getIndex() >= 0) {
                    path.append('[').append(reference.getIndex()).append(']');
                }
            }
            return path.isEmpty() ? null : path.toString();
        }
        return null;
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
