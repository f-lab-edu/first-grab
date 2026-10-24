package com.firstgrab.domain.stock.repository;

import com.firstgrab.domain.stock.StockDecreaseResult;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class StockRepository {

    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String ORDERED_KEY_PREFIX = "ordered:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> stockDecrementScript;

    public StockDecreaseResult decrease(Long eventId, Long userId, int quantity, Duration ttl) {
        validateTtl(ttl);
        Long result = stringRedisTemplate.execute(
                stockDecrementScript,
                List.of(createStockKey(eventId), createOrderKey(eventId)),
                String.valueOf(quantity), String.valueOf(userId), String.valueOf(ttl.toSeconds()));
        return StockDecreaseResult.fromCode(result);
    }

    public boolean init(Long eventId, int totalStock, Duration ttl) {
        validateTtl(ttl);
        Boolean saved = stringRedisTemplate.opsForValue().setIfAbsent(
                createStockKey(eventId), String.valueOf(totalStock), ttl);
        return Boolean.TRUE.equals(saved);
    }

    private void validateTtl(Duration ttl) {
        if (ttl.toSeconds() <= 0) {
            throw new IllegalArgumentException("TTL은 0보다 커야 합니다. ttl=" + ttl);
        }
    }

    private String createStockKey(Long eventId) {
        return STOCK_KEY_PREFIX + eventId;
    }

    private String createOrderKey(Long eventId) {
        return ORDERED_KEY_PREFIX + eventId;
    }
}
