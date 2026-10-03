package com.firstgrab.global.exception;

import com.firstgrab.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static com.firstgrab.global.exception.ErrorMessage.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("[BusinessException] {} {} - {}: {}",
                request.getMethod(), request.getRequestURI(),
                e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.of(e.getStatus().value(), e.getMessage(), null));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().getFirst();
        log.warn("[ValidationException] - field={}, message={}",
                fieldError.getField(), fieldError.getDefaultMessage());
        return new ResponseEntity<>(
                ApiResponse.of(status.value(), fieldError.getDefaultMessage(), null), headers, status
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("[DataIntegrityViolation] {} {} - {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.of(HttpStatus.CONFLICT.value(), DUPLICATE_DATA, null));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        return new ResponseEntity<>(
                ApiResponse.of(statusCode.value(), ex.getMessage(), null), headers, statusCode
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("[UnhandledException] {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), INTERNAL_SERVER_ERROR, null));
    }
}
