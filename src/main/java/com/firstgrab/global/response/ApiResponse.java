package com.firstgrab.global.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    public static final String SIGNUP_SUCCESS = "회원가입이 완료되었습니다.";

    private int status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> of(int status, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = status;
        response.message = message;
        response.data = data;
        return response;
    }
}
