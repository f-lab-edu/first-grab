package com.firstgrab.global.jwt;

import com.firstgrab.domain.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String TYPE_CLAIM = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final JwtParser accessJwtParser;
    private final JwtParser refreshJwtParser;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(@Value("${jwt.access-token-secret}") String accessSecret,
                       @Value("${jwt.refresh-token-secret}") String refreshSecret,
                       @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                       @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.accessSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
        this.refreshSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
        this.accessJwtParser = Jwts.parser().verifyWith(accessSecretKey).build();
        this.refreshJwtParser = Jwts.parser().verifyWith(refreshSecretKey).build();
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createAccessToken(Long userId, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .claim(TYPE_CLAIM, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(accessSecretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TYPE_CLAIM, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(refreshSecretKey)
                .compact();
    }

    public Optional<TokenClaims> parseToken(String token) {
        return parseClaims(accessJwtParser, token)
                .filter(claims -> TYPE_ACCESS.equals(claims.get(TYPE_CLAIM, String.class)))
                .map(this::toTokenClaims);
    }

    public Optional<Long> parseRefreshToken(String token) {
        return parseClaims(refreshJwtParser, token)
                .filter(claims -> TYPE_REFRESH.equals(claims.get(TYPE_CLAIM, String.class)))
                .map(claims -> Long.valueOf(claims.getSubject()));
    }

    private Optional<Claims> parseClaims(JwtParser jwtParser, String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return Optional.of(claims);
        } catch (ExpiredJwtException e) {
            log.debug("[JwtException] Expired token");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JwtException] Invalid token - type={}, message={}",
                    e.getClass().getSimpleName(), e.getMessage());
        }
        return Optional.empty();
    }

    private TokenClaims toTokenClaims(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
        return new TokenClaims(userId, role);
    }
}
