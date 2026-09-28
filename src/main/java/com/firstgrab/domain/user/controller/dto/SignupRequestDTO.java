package com.firstgrab.domain.user.controller.dto;

import com.firstgrab.domain.user.service.dto.SignupCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDTO {

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(min = 8, max = 20)
    private String password;

    @NotBlank
    @Size(max = 20)
    private String name;

    public SignupCommand toCommand() {
        return new SignupCommand(email, password, name);
    }
}
