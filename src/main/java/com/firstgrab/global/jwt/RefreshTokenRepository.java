package com.firstgrab.global.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate stringRedisTemplate;
    private final Duration refreshTokenExpiration;

    public RefreshTokenRepository(StringRedisTemplate stringRedisTemplate,
                                  @Value("${jwt.refresh-token-expiration}") Duration refreshTokenExpiration) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public void save(Long userId, String refreshToken) {
        stringRedisTemplate.opsForValue()
                .set(createKey(userId), refreshToken, refreshTokenExpiration);
    }

    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue()
                .get(createKey(userId)));
    }

    public void deleteByUserId(Long userId) {
        stringRedisTemplate.delete(createKey(userId));
    }

    private String createKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
