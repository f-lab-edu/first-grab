package com.firstgrab.domain.user.controller;

import com.firstgrab.domain.user.controller.dto.LoginRequestDTO;
import com.firstgrab.domain.user.controller.dto.LoginResponseDTO;
import com.firstgrab.domain.user.controller.dto.RefreshTokenRequestDTO;
import com.firstgrab.domain.user.controller.dto.ReissueResponseDTO;
import com.firstgrab.domain.user.controller.dto.SignupRequestDTO;
import com.firstgrab.domain.user.service.UserService;
import com.firstgrab.domain.user.service.dto.LoginResult;
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
        userService.signup(signupRequestDTO.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value(), ApiResponse.SIGNUP_SUCCESS, null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        LoginResult loginResult = userService.login(loginRequestDTO.toCommand());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.of(HttpStatus.OK.value(), ApiResponse.LOGIN_SUCCESS, LoginResponseDTO.from(loginResult)));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueResponseDTO>> reissue(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        String accessToken = userService.reissue(refreshTokenRequestDTO.getRefreshToken());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.of(HttpStatus.OK.value(), ApiResponse.REISSUE_SUCCESS, ReissueResponseDTO.from(accessToken)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        userService.logout(refreshTokenRequestDTO.getRefreshToken());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.of(HttpStatus.OK.value(), ApiResponse.LOGOUT_SUCCESS, null));
    }
}
