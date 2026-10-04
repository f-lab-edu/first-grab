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

    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                       @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
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
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TYPE_CLAIM, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Optional<TokenClaims> parseToken(String token) {
        return parseClaims(token).filter(claims -> claims.get(TYPE_CLAIM, String.class).equals(TYPE_ACCESS))
                    .map(this::toTokenClaims);
    }

    public Optional<Long> parseRefreshToken(String token) {
        return parseClaims(token).filter(claims -> claims.get(TYPE_CLAIM, String.class).equals(TYPE_REFRESH))
                .map(claims -> Long.valueOf(claims.getSubject()));
    }

    private Optional<Claims> parseClaims(String token){
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
