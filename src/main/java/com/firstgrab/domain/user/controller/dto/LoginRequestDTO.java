package com.firstgrab.domain.user.controller.dto;

import com.firstgrab.domain.user.service.dto.LoginCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class LoginRequestDTO {

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(min = 8, max = 20)
    private String password;

    public LoginCommand toCommand(){
        return new LoginCommand(email, password);
    }
}
