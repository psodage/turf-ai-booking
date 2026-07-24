package com.turfai.booking.exception;

import com.turfai.booking.dto.response.ErrorResponse;
import com.turfai.booking.dto.response.FieldErrorDetail;
import com.turfai.booking.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Global Exception Handler capturing all exceptions thrown across Controller layers.
 * Converts domain exceptions, validation errors, and unhandled failures into standard JSON ErrorResponse structures.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Domain exception [{}]: {} (Path: {}, CorrelationId: {})",
                errorCode.name(), ex.getMessage(), request.getRequestURI(), correlationId);

        ErrorResponse response = ErrorResponse.of(
                errorCode.name(),
                ex.getMessage(),
                correlationId,
                null,
                ex.getSuggestions()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = getCorrelationId();
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        log.warn("Validation failed for request to {} (CorrelationId: {}, Errors: {})",
                request.getRequestURI(), correlationId, fieldErrors.size());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_FAILED.name(),
                ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                correlationId,
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String correlationId = getCorrelationId();
        log.warn("Malformed JSON payload received at {} (CorrelationId: {})", request.getRequestURI(), correlationId);

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST.name(),
                "Malformed JSON request body.",
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception ex, HttpServletRequest request) {
        String correlationId = getCorrelationId();
        log.error("Unhandled exception at {} (CorrelationId: {})", request.getRequestURI(), correlationId, ex);

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private FieldErrorDetail mapFieldError(FieldError fieldError) {
        return new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private String getCorrelationId() {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        return correlationId != null ? correlationId : "N/A";
    }
}
