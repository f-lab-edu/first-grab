package com.firstgrab.domain.queue.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class QueueRepository {

    private static final String KEY_PREFIX = "queue:";

    private final StringRedisTemplate stringRedisTemplate;

    public QueueRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void addIfAbsent(Long eventId, Long userId, long enteredAt) {
        stringRedisTemplate.opsForZSet()
                .addIfAbsent(createKey(eventId), String.valueOf(userId), enteredAt);
    }

    public Optional<Long> findRank(Long eventId, Long userId) {
        return Optional.ofNullable(stringRedisTemplate.opsForZSet()
                .rank(createKey(eventId), String.valueOf(userId)));
    }

    private String createKey(Long eventId) {
        return KEY_PREFIX + eventId;
    }
}
