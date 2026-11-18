package com.firstgrab.domain.stock.repository;

import com.firstgrab.domain.stock.OrderReservation;
import com.firstgrab.domain.stock.OrderReserveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class StockRepository {

    private static final String ACTIVE_KEY_PREFIX = "active:";
    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String BOUGHT_KEY_PREFIX = "bought:";
    private static final String RESERVED_KEY_PREFIX = "reserved:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> orderReserveScript;

    public OrderReserveResult reserve(OrderReservation reservation, Instant now, Instant expireAt) {
        validatePositive("수량", reservation.quantity());
        validatePositive("1인 구매 한도", reservation.perUserLimit());
        validateExpireAt(expireAt, now);

        Long eventId = reservation.eventId();
        Long result = stringRedisTemplate.execute(
                orderReserveScript,
                List.of(createActiveKey(eventId), createStockKey(eventId),
                        createBoughtKey(eventId), createReservedKey(eventId)),
                String.valueOf(reservation.userId()),
                String.valueOf(reservation.quantity()),
                String.valueOf(reservation.perUserLimit()),
                String.valueOf(now.toEpochMilli()),
                String.valueOf(expireAt.getEpochSecond()),
                reservation.orderId());
        return OrderReserveResult.fromCode(result);
    }

    public boolean init(Long eventId, int totalStock, Duration ttl) {
        validateTtl(ttl);
        Boolean saved = stringRedisTemplate.opsForValue().setIfAbsent(
                createStockKey(eventId), String.valueOf(totalStock), ttl);
        return Boolean.TRUE.equals(saved);
    }

    private void validatePositive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " 값은 0보다 커야 합니다. value=" + value);
        }
    }

    private void validateExpireAt(Instant expireAt, Instant now) {
        if (!expireAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "만료 시각은 현재 이후여야 합니다. expireAt=" + expireAt + ", now=" + now);
        }
    }

    private void validateTtl(Duration ttl) {
        if (ttl.toSeconds() <= 0) {
            throw new IllegalArgumentException("TTL은 0보다 커야 합니다. ttl=" + ttl);
        }
    }

    private String createActiveKey(Long eventId) {
        return ACTIVE_KEY_PREFIX + "{" + eventId + "}";
    }

    private String createStockKey(Long eventId) {
        return STOCK_KEY_PREFIX + "{" + eventId + "}";
    }

    private String createBoughtKey(Long eventId) {
        return BOUGHT_KEY_PREFIX + "{" + eventId + "}";
    }

    private String createReservedKey(Long eventId) {
        return RESERVED_KEY_PREFIX + "{" + eventId + "}";
    }
}
