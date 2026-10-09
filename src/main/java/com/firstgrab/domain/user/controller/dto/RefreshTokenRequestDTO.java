package com.firstgrab.domain.user.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshTokenRequestDTO {

    @NotBlank
    private String refreshToken;
}
