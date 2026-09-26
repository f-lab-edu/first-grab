package com.firstgrab.global.exception;

import com.firstgrab.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.firstgrab.global.exception.ErrorMessage.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e, HttpServletRequest request){
        log.warn("[BusinessException] {} {} - {}: {}",
                request.getMethod(), request.getRequestURI(),
                e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.of(e.getStatus().value(), e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().getFirst();
        log.warn("[ValidationException] {} {} - field={}, message={}",
                request.getMethod(), request.getRequestURI(),
                fieldError.getField(), fieldError.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.of(HttpStatus.BAD_REQUEST.value(), fieldError.getDefaultMessage(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("[DataIntegrityViolation] {} {} - {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.of(HttpStatus.CONFLICT.value(), DUPLICATE_DATA, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request){
        log.error("[UnhandledException] {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), INTERNAL_SERVER_ERROR, null));
    }
}
