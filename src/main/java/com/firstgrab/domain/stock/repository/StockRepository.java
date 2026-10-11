package com.firstgrab.domain.stock.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class StockRepository {

    private static final String KEY_PREFIX = "stock:";
    private static final long OUT_OF_STOCK = -1L;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> stockDecrementScript;

    public boolean decrease(Long eventId, int quantity) {
        Long result = stringRedisTemplate.execute(
                stockDecrementScript,
                List.of(createKey(eventId)),
                String.valueOf(quantity));
        return result != null && result != OUT_OF_STOCK;
    }

    private String createKey(Long eventId) {
        return KEY_PREFIX + eventId;
    }
}
