package com.firstgrab.domain.user.service;

import com.firstgrab.domain.user.entity.Role;
import com.firstgrab.domain.user.entity.User;
import com.firstgrab.domain.user.repository.UserRepository;
import com.firstgrab.domain.user.service.dto.LoginCommand;
import com.firstgrab.domain.user.service.dto.LoginResult;
import com.firstgrab.domain.user.service.dto.SignupCommand;
import com.firstgrab.global.exception.DuplicateException;
import com.firstgrab.global.exception.UnauthorizedException;
import com.firstgrab.global.jwt.JwtProvider;
import com.firstgrab.global.jwt.RefreshTokenHasher;
import com.firstgrab.global.jwt.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.firstgrab.global.exception.ErrorMessage.DUPLICATE_EMAIL;
import static com.firstgrab.global.exception.ErrorMessage.INVALID_LOGIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private static final String NAME = "홍길동";
    private static final String EMAIL = "test@test.com";
    private static final String RAW_PASSWORD = "12345678";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String HASHED_REFRESH_TOKEN = "hashedRefreshToken";

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Test
    @DisplayName("회원가입 성공")
    void signupSuccess() {
        SignupCommand signupCommand = new SignupCommand(EMAIL, RAW_PASSWORD, NAME);
        when(userRepository.findByEmail(signupCommand.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(signupCommand.getPassword())).thenReturn(ENCODED_PASSWORD);

        userService.signup(signupCommand);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("이메일 중복 시 DuplicateException")
    void signupDuplicateEmail() {
        SignupCommand signupCommand = new SignupCommand(EMAIL, RAW_PASSWORD, NAME);
        when(userRepository.findByEmail(signupCommand.getEmail())).thenReturn(Optional.of(mock(User.class)));

        assertThatThrownBy(() -> userService.signup(signupCommand))
                .isInstanceOf(DuplicateException.class)
                .hasMessage(DUPLICATE_EMAIL);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공 시 accessToken, refreshToken 반환 및 저장")
    void loginSuccess() {
        User user = User.createUser(EMAIL, ENCODED_PASSWORD, NAME);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtProvider.createAccessToken(user.getId(), Role.USER)).thenReturn(ACCESS_TOKEN);
        when(jwtProvider.createRefreshToken(user.getId())).thenReturn(REFRESH_TOKEN);
        when(refreshTokenHasher.hash(REFRESH_TOKEN)).thenReturn(HASHED_REFRESH_TOKEN);

        LoginResult result = userService.login(new LoginCommand(EMAIL, RAW_PASSWORD));

        assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.getRefreshToken()).isEqualTo(REFRESH_TOKEN);

        verify(refreshTokenRepository).save(user.getId(), HASHED_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 UnauthorizedException")
    void loginUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginCommand(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(INVALID_LOGIN);

        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("탈퇴한 회원이면 UnauthorizedException")
    void loginDeletedUser() {
        User deletedUser = mock(User.class);
        when(deletedUser.getDeletedAt()).thenReturn(LocalDateTime.now());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(deletedUser));

        assertThatThrownBy(() -> userService.login(new LoginCommand(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(INVALID_LOGIN);

        verifyNoInteractions(passwordEncoder, jwtProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 UnauthorizedException")
    void loginPasswordMismatch() {
        User user = User.createUser(EMAIL, ENCODED_PASSWORD, NAME);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginCommand(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(INVALID_LOGIN);

        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

}
