package com.firstgrab.domain.user.controller.dto;

import com.firstgrab.domain.user.service.dto.LoginCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequestDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    public LoginCommand toCommand(){
        return new LoginCommand(email, password);
    }
}
