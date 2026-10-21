package com.firstgrab.domain.stock.repository;

import com.firstgrab.domain.stock.StockDecreaseResult;
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

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.firstgrab.domain.stock.StockDecreaseResult.ALREADY_ORDERED;
import static com.firstgrab.domain.stock.StockDecreaseResult.SOLD_OUT;
import static com.firstgrab.domain.stock.StockDecreaseResult.STOCK_NOT_FOUND;
import static com.firstgrab.domain.stock.StockDecreaseResult.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataRedisTest
@Import({RedisContainerConfig.class, StockRepository.class, RedisScriptConfig.class})
public class StockRepositoryTest {

    private static final Long EVENT_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Duration TTL = Duration.ofHours(1);
    private static final String STOCK_KEY = "stock:{" + EVENT_ID + "}";
    private static final String ORDERED_KEY = "ordered:{" + EVENT_ID + "}";

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    @DisplayName("재고 100개에 1000명이 동시에 주문하면 100명만 성공")
    void decreaseConcurrently() throws InterruptedException {
        int stock = 100;
        int requestCount = 1000;
        stringRedisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(stock));

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(requestCount);
        Queue<StockDecreaseResult> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < requestCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    results.add(stockRepository.decrease(EVENT_ID, userId, 1, TTL));
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
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 차감 실패")
    void decreaseNotEnoughStock() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "2");

        StockDecreaseResult result = stockRepository.decrease(EVENT_ID, USER_ID, 3, TTL);

        assertThat(result).isEqualTo(SOLD_OUT);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("2");
    }

    @Test
    @DisplayName("수량이 0 이하면 차감 실패")
    void decreaseInvalidQuantity() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");

        StockDecreaseResult result = stockRepository.decrease(EVENT_ID, USER_ID, 0, TTL);

        assertThat(result).isEqualTo(SOLD_OUT);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("10");
    }

    @Test
    @DisplayName("같은 사용자가 동시에 여러 번 주문하면 1건만 성공")
    void decreaseAlreadyOrdered() throws InterruptedException {
        int stock = 10;
        int requestCount = 5;
        stringRedisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(stock));

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch latch = new CountDownLatch(requestCount);
        Queue<StockDecreaseResult> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    results.add(stockRepository.decrease(EVENT_ID, USER_ID, 1, TTL));
                } finally {
                    latch.countDown();
                }
            });
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executorService.shutdown();

        assertThat(results).hasSize(requestCount);
        assertThat(results).filteredOn(result -> result == SUCCESS).hasSize(1);
        assertThat(results).filteredOn(result -> result == ALREADY_ORDERED).hasSize(requestCount - 1);
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo(String.valueOf(stock - 1));
    }

    @Test
    @DisplayName("재고 키가 없으면 STOCK_NOT_FOUND를 반환")
    void decreaseStockNotFound() {
        StockDecreaseResult result = stockRepository.decrease(EVENT_ID, USER_ID, 1, TTL);

        assertThat(result).isEqualTo(STOCK_NOT_FOUND);
        assertThat(stringRedisTemplate.hasKey(STOCK_KEY)).isFalse();
    }

    @Test
    @DisplayName("TTL이 0 이하면 IllegalArgumentException")
    void decreaseInvalidTtl() {
        assertThatThrownBy(() -> stockRepository.decrease(EVENT_ID, USER_ID, 1, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TTL은 0보다 커야 합니다. ttl=PT0S");

        assertThat(stringRedisTemplate.hasKey(ORDERED_KEY)).isFalse();
    }

    @Test
    @DisplayName("구매자 키 TTL은 첫 주문에만 설정")
    void decreaseSetTtlOnce() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");

        stockRepository.decrease(EVENT_ID, 1L, 1, TTL);
        stockRepository.decrease(EVENT_ID, 2L, 1, Duration.ofHours(2));

        Long ttl = stringRedisTemplate.getExpire(ORDERED_KEY);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(TTL.toSeconds());
    }

    @Test
    @DisplayName("재고 초기화 성공")
    void initSuccess() {
        boolean result = stockRepository.init(EVENT_ID, 100, TTL);

        assertThat(result).isTrue();
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("100");
        assertThat(stringRedisTemplate.getExpire(STOCK_KEY)).isPositive();
    }

    @Test
    @DisplayName("재고 키가 이미 있으면 덮어쓰지 않음")
    void initAlreadyExists() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "100");

        boolean result = stockRepository.init(EVENT_ID, 50, TTL);

        assertThat(result).isFalse();
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("100");
    }
}
