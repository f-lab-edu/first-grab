package com.firstgrab.domain.stock.repository;

import com.firstgrab.domain.stock.OrderReservation;
import com.firstgrab.domain.stock.OrderReserveResult;
import com.firstgrab.global.config.RedisScriptConfig;
import com.firstgrab.support.RedisContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.firstgrab.domain.stock.OrderReserveResult.DUPLICATE_ORDER_ID;
import static com.firstgrab.domain.stock.OrderReserveResult.LIMIT_EXCEEDED;
import static com.firstgrab.domain.stock.OrderReserveResult.NOT_ADMITTED;
import static com.firstgrab.domain.stock.OrderReserveResult.SOLD_OUT;
import static com.firstgrab.domain.stock.OrderReserveResult.STOCK_NOT_FOUND;
import static com.firstgrab.domain.stock.OrderReserveResult.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataRedisTest
@Import({RedisContainerConfig.class, StockRepository.class, RedisScriptConfig.class})
public class StockRepositoryTest {

    private static final Long EVENT_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final int PER_USER_LIMIT = 5;
    private static final String ACTIVE_KEY = "active:{" + EVENT_ID + "}";
    private static final String STOCK_KEY = "stock:{" + EVENT_ID + "}";
    private static final String BOUGHT_KEY = "bought:{" + EVENT_ID + "}";
    private static final String RESERVED_KEY = "reserved:{" + EVENT_ID + "}";
    private static final Duration STOCK_TTL = Duration.ofHours(1);

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Instant now;
    private Instant expireAt;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        now = Instant.now();
        expireAt = now.plus(Duration.ofHours(1));
    }

    @Test
    @DisplayName("재고 100개에 1000명이 동시에 주문하면 100명만 성공")
    void reserveConcurrently() throws InterruptedException {
        int stock = 100;
        int requestCount = 1000;
        stringRedisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(stock));
        for (long userId = 0; userId < requestCount; userId++) {
            admit(userId);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(requestCount);
        Queue<OrderReserveResult> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < requestCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    results.add(stockRepository.reserve(reservation(userId, 1), now, expireAt));
                } finally {
                    latch.countDown();
                }
            });
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executorService.shutdown();

        String remaining = stringRedisTemplate.opsForValue().get(STOCK_KEY);
        assertThat(results).hasSize(requestCount);
        assertThat(results).filteredOn(result -> result == SUCCESS).hasSize(stock);
        assertThat(results).filteredOn(result -> result == SOLD_OUT).hasSize(requestCount - stock);
        assertThat(remaining).isEqualTo("0");
        assertThat(stringRedisTemplate.opsForHash().size(BOUGHT_KEY)).isEqualTo(stock);
        assertThat(stringRedisTemplate.opsForHash().size(RESERVED_KEY)).isEqualTo(stock);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 예약 실패")
    void reserveSoldOut() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "2");
        admit(USER_ID);

        OrderReserveResult result = stockRepository.reserve(reservation(USER_ID, 3), now, expireAt);

        assertThat(result).isEqualTo(SOLD_OUT);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("2");
        assertNothingReserved(USER_ID);
    }

    @Test
    @DisplayName("수량이 0 이하면 예외")
    void reserveInvalidQuantity() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");
        admit(USER_ID);

        assertThatThrownBy(() -> stockRepository.reserve(reservation(USER_ID, 0), now, expireAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수량");

        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("10");
        assertNothingReserved(USER_ID);
    }

    @Test
    @DisplayName("같은 사용자가 동시에 여러 번 주문하면 입장권이 한 번만 사용됨")
    void reserveConcurrentlySameUser() throws InterruptedException {
        int stock = 10;
        int requestCount = 5;
        stringRedisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(stock));
        admit(USER_ID);

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch latch = new CountDownLatch(requestCount);
        Queue<OrderReserveResult> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    results.add(stockRepository.reserve(reservation(USER_ID, 1), now, expireAt));
                } finally {
                    latch.countDown();
                }
            });
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executorService.shutdown();

        assertThat(results).hasSize(requestCount);
        assertThat(results).filteredOn(result -> result == SUCCESS).hasSize(1);
        assertThat(results).filteredOn(result -> result == NOT_ADMITTED).hasSize(requestCount - 1);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo(String.valueOf(stock - 1));
        assertThat(stringRedisTemplate.opsForHash().get(BOUGHT_KEY, String.valueOf(USER_ID))).isEqualTo("1");
        assertThat(stringRedisTemplate.opsForHash().size(RESERVED_KEY)).isEqualTo(1);
        assertThat(stringRedisTemplate.opsForZSet().score(ACTIVE_KEY, String.valueOf(USER_ID))).isNull();
    }

    @Test
    @DisplayName("재고 키가 없으면 STOCK_NOT_FOUND")
    void reserveStockNotFound() {
        admit(USER_ID);

        OrderReserveResult result = stockRepository.reserve(reservation(USER_ID, 1), now, expireAt);

        assertThat(result).isEqualTo(STOCK_NOT_FOUND);
        assertThat(stringRedisTemplate.hasKey(STOCK_KEY)).isFalse();
        assertNothingReserved(USER_ID);
    }

    @Test
    @DisplayName("만료 시각이 현재 이후가 아니면 예외")
    void reserveInvalidExpireAt() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");
        admit(USER_ID);

        assertThatThrownBy(() -> stockRepository.reserve(reservation(USER_ID, 1), now, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료 시각");

        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("10");
        assertNothingReserved(USER_ID);
    }

    @Test
    @DisplayName("bought, reserved 만료 시각은 첫 예약에만 설정")
    void reserveSetExpireAtOnce() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");
        admit(1L);
        admit(2L);

        OrderReserveResult first = stockRepository.reserve(reservation(1L, 1), now, expireAt);
        OrderReserveResult second = stockRepository.reserve(reservation(2L, 1), now, expireAt.plus(Duration.ofHours(1)));

        assertThat(first).isEqualTo(SUCCESS);
        assertThat(second).isEqualTo(SUCCESS);
        assertThat(expireTime(BOUGHT_KEY)).isEqualTo(expireAt.getEpochSecond());
        assertThat(expireTime(RESERVED_KEY)).isEqualTo(expireAt.getEpochSecond());
    }

    @Test
    @DisplayName("재고 초기화 성공")
    void initSuccess() {
        boolean result = stockRepository.init(EVENT_ID, 100, STOCK_TTL);

        assertThat(result).isTrue();
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("100");
        assertThat(stringRedisTemplate.getExpire(STOCK_KEY)).isPositive();
    }

    @Test
    @DisplayName("재고 키가 이미 있으면 덮어쓰지 않음")
    void initAlreadyExists() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "100");

        boolean result = stockRepository.init(EVENT_ID, 50, STOCK_TTL);

        assertThat(result).isFalse();
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("100");
    }

    @Test
    @DisplayName("입장권이 없으면 NOT_ADMITTED")
    void reserveNotAdmitted() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "100");

        OrderReserveResult result = stockRepository.reserve(reservation(USER_ID, 1), now, expireAt);

        assertThat(result).isEqualTo(NOT_ADMITTED);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("100");
        assertThat(stringRedisTemplate.hasKey(BOUGHT_KEY)).isFalse();
        assertThat(stringRedisTemplate.hasKey(RESERVED_KEY)).isFalse();
    }

    @Test
    @DisplayName("입장권이 만료됐으면 NOT_ADMITTED")
    void reserveAdmitExpired() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");
        stringRedisTemplate.opsForZSet().add(ACTIVE_KEY, String.valueOf(USER_ID),
                now.minus(Duration.ofMinutes(5)).toEpochMilli());

        OrderReserveResult result = stockRepository.reserve(reservation(USER_ID, 1), now, expireAt);

        assertThat(result).isEqualTo(NOT_ADMITTED);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("10");
        assertNothingReserved(USER_ID);
    }

    @Test
    @DisplayName("누적 수량이 한도를 넘으면 LIMIT_EXCEEDED")
    void reserveLimitExceeded() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");
        admit(USER_ID);
        stringRedisTemplate.opsForHash().put(BOUGHT_KEY, String.valueOf(USER_ID), "4");

        OrderReserveResult result = stockRepository.reserve(reservation(USER_ID, 2), now, expireAt);

        assertThat(result).isEqualTo(LIMIT_EXCEEDED);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("10");
        assertThat(stringRedisTemplate.opsForHash().get(BOUGHT_KEY, String.valueOf(USER_ID))).isEqualTo("4");
        assertThat(stringRedisTemplate.opsForZSet().score(ACTIVE_KEY, String.valueOf(USER_ID))).isNotNull();
        assertThat(stringRedisTemplate.hasKey(RESERVED_KEY)).isFalse();
    }

    @Test
    @DisplayName("누적 수량이 한도와 같으면 성공")
    void reserveLimitBoundary() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");
        admit(USER_ID);
        stringRedisTemplate.opsForHash().put(BOUGHT_KEY, String.valueOf(USER_ID), "3");

        OrderReserveResult result = stockRepository.reserve(reservation(USER_ID, 2), now, expireAt);

        assertThat(result).isEqualTo(SUCCESS);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("8");
        assertThat(stringRedisTemplate.opsForHash().get(BOUGHT_KEY, String.valueOf(USER_ID))).isEqualTo("5");
    }

    @Test
    @DisplayName("한 번 성공하면 같은 사용자의 재요청은 NOT_ADMITTED")
    void reserveConsumedTicket() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");
        admit(USER_ID);

        OrderReserveResult first = stockRepository.reserve(reservation(USER_ID, 1), now, expireAt);
        OrderReserveResult second = stockRepository.reserve(reservation(USER_ID, 1), now, expireAt);

        assertThat(first).isEqualTo(SUCCESS);
        assertThat(second).isEqualTo(NOT_ADMITTED);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("9");
        assertThat(stringRedisTemplate.opsForHash().get(BOUGHT_KEY, String.valueOf(USER_ID))).isEqualTo("1");
        assertThat(stringRedisTemplate.opsForHash().size(RESERVED_KEY)).isEqualTo(1);
    }

    @Test
    @DisplayName("예약 기록이 userId:quantity:admitUntil 형식으로 저장됨")
    void reserveSavesReservation() {
        String orderId = "order-1";
        long admitUntil = now.plus(Duration.ofMinutes(5)).toEpochMilli();
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");
        stringRedisTemplate.opsForZSet().add(ACTIVE_KEY, String.valueOf(USER_ID), admitUntil);

        OrderReserveResult result = stockRepository.reserve(
                new OrderReservation(orderId, EVENT_ID, USER_ID, 2, PER_USER_LIMIT), now, expireAt);

        assertThat(result).isEqualTo(SUCCESS);
        assertThat(stringRedisTemplate.opsForHash().get(RESERVED_KEY, orderId))
                .isEqualTo(USER_ID + ":2:" + admitUntil);
        assertThat(stringRedisTemplate.opsForZSet().score(ACTIVE_KEY, String.valueOf(USER_ID))).isNull();
    }

    @Test
    @DisplayName("같은 orderId면 DUPLICATE_ORDER_ID")
    void reserveDuplicateOrderId() {
        String orderId = "order-1";
        String existing = "2:1:" + now.plus(Duration.ofMinutes(5)).toEpochMilli();
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");
        admit(USER_ID);
        stringRedisTemplate.opsForHash().put(RESERVED_KEY, orderId, existing);

        OrderReserveResult result = stockRepository.reserve(
                new OrderReservation(orderId, EVENT_ID, USER_ID, 1, PER_USER_LIMIT), now, expireAt);

        assertThat(result).isEqualTo(DUPLICATE_ORDER_ID);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("10");
        assertThat(stringRedisTemplate.opsForHash().get(RESERVED_KEY, orderId)).isEqualTo(existing);
        assertThat(stringRedisTemplate.opsForHash().hasKey(BOUGHT_KEY, String.valueOf(USER_ID))).isFalse();
        assertThat(stringRedisTemplate.opsForZSet().score(ACTIVE_KEY, String.valueOf(USER_ID))).isNotNull();
    }

    private void admit(long userId) {
        stringRedisTemplate.opsForZSet().add(ACTIVE_KEY, String.valueOf(userId),
                now.plus(Duration.ofMinutes(5)).toEpochMilli());
    }

    private OrderReservation reservation(long userId, int quantity) {
        return new OrderReservation(UUID.randomUUID().toString(), EVENT_ID, userId, quantity, PER_USER_LIMIT);
    }

    private void assertNothingReserved(Long userId) {
        assertThat(stringRedisTemplate.opsForZSet().score(ACTIVE_KEY, String.valueOf(userId))).isNotNull();
        assertThat(stringRedisTemplate.opsForHash().hasKey(BOUGHT_KEY, String.valueOf(userId))).isFalse();
        assertThat(stringRedisTemplate.hasKey(RESERVED_KEY)).isFalse();
    }

    private Long expireTime(String key) {
        return stringRedisTemplate.execute(
                RedisScript.of("return redis.call('EXPIRETIME', KEYS[1])", Long.class),
                List.of(key));
    }
}
