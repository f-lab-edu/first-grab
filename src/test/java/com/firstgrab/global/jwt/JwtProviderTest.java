package com.firstgrab.global.jwt;

import com.firstgrab.domain.user.entity.Role;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET =
            Encoders.BASE64.encode("test-secret-key-for-jwt-provider-test".getBytes());
    private static final String OTHER_SECRET =
            Encoders.BASE64.encode("other-secret-key-for-jwt-provider-test".getBytes());
    private static final long EXPIRATION = 3_600_000L;
    private static final Long USER_ID = 1L;
    private static final Role ROLE = Role.USER;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, EXPIRATION);
    }

    @Test
    @DisplayName("jwt 토큰 생성 후 파싱 성공")
    void parseSuccess() {
        String token = jwtProvider.createAccessToken(USER_ID, ROLE);

        Optional<TokenClaims> result = jwtProvider.parseToken(token);

        assertThat(result).contains(new TokenClaims(USER_ID, ROLE));
    }

    @Test
    @DisplayName("만료된 토큰이면 empty를 반환")
    void parseExpired() {
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1000L);
        String token = expiredProvider.createAccessToken(USER_ID, ROLE);

        assertThat(jwtProvider.parseToken(token)).isEmpty();
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰이면 empty를 반환")
    void parseInvalidSignature() {
        JwtProvider otherProvider = new JwtProvider(OTHER_SECRET, EXPIRATION);
        String token = otherProvider.createAccessToken(USER_ID, ROLE);

        assertThat(jwtProvider.parseToken(token)).isEmpty();
    }

    @Test
    @DisplayName("형식이 잘못된 토큰이면 empty를 반환")
    void parseMalformed() {
        assertThat(jwtProvider.parseToken("invalid.token.value")).isEmpty();
    }

    @Test
    @DisplayName("null이거나 빈 문자열이면 empty를 반환")
    void parseEmpty() {
        assertThat(jwtProvider.parseToken(null)).isEmpty();
        assertThat(jwtProvider.parseToken("")).isEmpty();
    }
}
