package com.esteban.comunitymanager.config;

import com.esteban.comunitymanager.dto.response.ErrorResponse;
import com.esteban.comunitymanager.exception.MetaPublicacionException;
import com.esteban.comunitymanager.exception.PublicacionInmutableException;
import com.esteban.comunitymanager.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .codigo("NOT_FOUND")
                        .mensaje(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .codigo("VALIDATION_ERROR")
                        .mensaje(mensaje)
                        .build());
    }

    @ExceptionHandler(PublicacionInmutableException.class)
    public ResponseEntity<ErrorResponse> handlePublicacionInmutable(PublicacionInmutableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .codigo("CONFLICT")
                        .mensaje(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .codigo("BAD_REQUEST")
                        .mensaje(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .codigo("BAD_REQUEST")
                        .mensaje(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MetaPublicacionException.class)
    public ResponseEntity<ErrorResponse> handleMetaPublicacion(MetaPublicacionException ex) {
        HttpStatus status = ex.isErrorDatos() ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .codigo("META_ERROR")
                        .mensaje(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperation(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ErrorResponse.builder()
                        .codigo("NOT_IMPLEMENTED")
                        .mensaje(ex.getMessage())
                        .build());
    }
}
