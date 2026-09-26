package com.firstgrab.domain.user.controller;

import com.firstgrab.domain.user.controller.dto.SignupRequestDTO;
import com.firstgrab.domain.user.service.UserService;
import com.firstgrab.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequestDTO signupRequestDTO) {
        userService.signup(signupRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, ApiResponse.SIGNUP_SUCCESS, null));
    }

}
