package com.firstgrab.domain.user.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReissueResponseDTO {

    private final String accessToken;

    public static ReissueResponseDTO from(String accessToken) {
        return new ReissueResponseDTO(accessToken);
    }
}
