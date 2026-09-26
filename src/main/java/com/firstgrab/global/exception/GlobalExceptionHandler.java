package com.firstgrab.global.exception;

import com.firstgrab.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
                .body(ApiResponse.of(400, fieldError.getDefaultMessage(), null));
    }

}
