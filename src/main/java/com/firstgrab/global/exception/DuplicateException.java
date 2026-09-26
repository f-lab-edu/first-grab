package com.firstgrab.global.exception;

import org.springframework.http.HttpStatus;

public class DuplicateException extends BusinessException {
    public DuplicateException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
