package com.firstgrab.domain.user.controller.dto;

import com.firstgrab.domain.user.service.dto.LoginResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {

    private final String accessToken;
    private final String refreshToken;

    public static LoginResponseDTO from(LoginResult loginResult){
        return new LoginResponseDTO(loginResult.getAccessToken(), loginResult.getRefreshToken());
    }
}
