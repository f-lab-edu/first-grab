package com.firstgrab.domain.user.service;

import com.firstgrab.domain.user.controller.dto.SignupRequestDTO;
import com.firstgrab.domain.user.entity.User;
import com.firstgrab.domain.user.repository.UserRepository;
import com.firstgrab.global.exception.DuplicateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공")
    void signupSuccess() {
        SignupRequestDTO signupRequestDTO = new SignupRequestDTO("test@test.com", "12345678", "홍길동");
        when(userRepository.findByEmail(signupRequestDTO.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(signupRequestDTO.getPassword())).thenReturn("encodedPassword");

        userService.signup(signupRequestDTO);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("이메일 중복 시 DuplicateException")
    void signupDuplicateEmail() {
        SignupRequestDTO signupRequestDTO = new SignupRequestDTO("test@test.com", "12345678", "홍길동");
        when(userRepository.findByEmail(signupRequestDTO.getEmail())).thenReturn(Optional.of(mock(User.class)));

        assertThrows(DuplicateException.class, () -> {
            userService.signup(signupRequestDTO);
        });

        verify(userRepository, times(0)).save(any(User.class));
    }
}
