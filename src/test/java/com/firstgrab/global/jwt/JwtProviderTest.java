package com.firstgrab.global.jwt;

import com.firstgrab.domain.user.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String ACCESS_SECRET =
            Encoders.BASE64.encode("access-secret-key-for-jwt-provider-test".getBytes());
    private static final String REFRESH_SECRET =
            Encoders.BASE64.encode("refresh-secret-key-for-jwt-provider-test".getBytes());
    private static final String OTHER_SECRET =
            Encoders.BASE64.encode("other-secret-key-for-jwt-provider-test".getBytes());
    private static final long EXPIRATION = 3_600_000L;
    private static final long REFRESH_EXPIRATION = 1_209_600_000L;
    private static final Long USER_ID = 1L;
    private static final Role ROLE = Role.USER;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(ACCESS_SECRET, REFRESH_SECRET, EXPIRATION, REFRESH_EXPIRATION);
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
        JwtProvider expiredProvider = new JwtProvider(ACCESS_SECRET, REFRESH_SECRET, -1000L, REFRESH_EXPIRATION);
        String token = expiredProvider.createAccessToken(USER_ID, ROLE);

        assertThat(jwtProvider.parseToken(token)).isEmpty();
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰이면 empty를 반환")
    void parseInvalidSignature() {
        JwtProvider otherProvider = new JwtProvider(OTHER_SECRET, REFRESH_SECRET, EXPIRATION, REFRESH_EXPIRATION);
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

    @Test
    @DisplayName("refresh jwt 토큰 생성 후 파싱 성공")
    void parseRefreshTokenSuccess() {
        String token = jwtProvider.createRefreshToken(USER_ID);

        Optional<Long> userId = jwtProvider.parseRefreshToken(token);

        assertThat(userId).contains(USER_ID);
    }

    @Test
    @DisplayName("refresh 토큰으로 parseToken 호출하면 empty 반환")
    void parseTokenWithRefreshToken() {
        String token = jwtProvider.createRefreshToken(USER_ID);

        Optional<TokenClaims> result = jwtProvider.parseToken(token);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("access 토큰으로 parseRefreshToken 호출하면 empty 반환")
    void parseRefreshTokenWithAccessToken() {
        String token = jwtProvider.createAccessToken(USER_ID, ROLE);

        Optional<Long> userId = jwtProvider.parseRefreshToken(token);

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("type claim이 없는 access 키 토큰이면 parseToken은 empty를 반환")
    void parseTokenWithoutTypeClaim() {
        String token = createTokenWithoutType(ACCESS_SECRET);

        assertThat(jwtProvider.parseToken(token)).isEmpty();
    }

    @Test
    @DisplayName("type claim이 없는 refresh 키 토큰이면 parseRefreshToken은 empty를 반환")
    void parseRefreshTokenWithoutTypeClaim() {
        String token = createTokenWithoutType(REFRESH_SECRET);

        assertThat(jwtProvider.parseRefreshToken(token)).isEmpty();
    }

    private String createTokenWithoutType(String secret) {
        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        return Jwts.builder()
                .subject(String.valueOf(USER_ID))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(secretKey)
                .compact();
    }
}
