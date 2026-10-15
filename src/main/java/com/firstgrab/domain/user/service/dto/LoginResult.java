package com.firstgrab.domain.user.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResult {

    private final String accessToken;
    private final String refreshToken;
}
