package com.firstgrab.domain.user.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginCommand {

    private final String email;
    private final String password;
}
