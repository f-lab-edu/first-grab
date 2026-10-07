package com.firstgrab.domain.user.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.firstgrab.global.exception.ErrorMessage.DUPLICATE_EMAIL;
import static com.firstgrab.global.exception.ErrorMessage.INVALID_LOGIN;
import static com.firstgrab.global.exception.ErrorMessage.INVALID_REFRESH_TOKEN;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;

    @Transactional
    public void signup(SignupCommand signupCommand) {
        validateDuplicateEmail(signupCommand.getEmail());
        String encodedPassword = passwordEncoder.encode(signupCommand.getPassword());
        User user = User.createUser(signupCommand.getEmail(), encodedPassword, signupCommand.getName());
        userRepository.save(user);
        log.info("User signed up, userId={}", user.getId());
    }

    private void validateDuplicateEmail(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new DuplicateException(DUPLICATE_EMAIL);
                });
    }

    public LoginResult login(LoginCommand loginCommand) {
        User user = getLoginUser(loginCommand.getEmail());
        validateNotDeleted(user);
        validateMatchedPassword(loginCommand.getPassword(), user.getPassword());

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        String hashedRefreshToken = refreshTokenHasher.hash(refreshToken);
        refreshTokenRepository.save(user.getId(), hashedRefreshToken);

        log.info("User logged in, userId={}", user.getId());
        return new LoginResult(accessToken, refreshToken);
    }

    private User getLoginUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(INVALID_LOGIN));
    }

    private void validateNotDeleted(User user) {
        if (user.getDeletedAt() != null) {
            throw new UnauthorizedException(INVALID_LOGIN);
        }
    }

    private void validateMatchedPassword(String inputPassword, String password) {
        if (!passwordEncoder.matches(inputPassword, password)) {
            throw new UnauthorizedException(INVALID_LOGIN);
        }
    }

    @Transactional(readOnly = true)
    public String reissue(String refreshToken) {
        Long userId = getUserIdFromRefreshToken(refreshToken);
        validateStoredRefreshToken(userId, refreshToken);
        User user = getActiveUser(userId);
        String accessToken = jwtProvider.createAccessToken(userId, user.getRole());
        log.info("Access token reissued, userId={}", userId);
        return accessToken;
    }

    private Long getUserIdFromRefreshToken(String refreshToken) {
        return jwtProvider.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN));
    }

    private void validateStoredRefreshToken(Long userId, String refreshToken) {
        refreshTokenRepository.findByUserId(userId)
                .filter(token -> token.equals(refreshToken))
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN));
    }

    private User getActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN));
    }

    public void logout(String refreshToken) {
        Long userId = getUserIdFromRefreshToken(refreshToken);
        validateStoredRefreshToken(userId, refreshToken);
        refreshTokenRepository.deleteByUserId(userId);
        log.info("User logged out, userId={}", userId);
    }
}
