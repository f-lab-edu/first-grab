package com.firstgrab.domain.stock.repository;

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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import({RedisContainerConfig.class, StockRepository.class, RedisScriptConfig.class})
public class StockRepositoryTest {

    private static final Long EVENT_ID = 1L;
    private static final String STOCK_KEY = "stock:" + EVENT_ID;

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
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    if (stockRepository.decrease(EVENT_ID, 1)) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        String remaining = stringRedisTemplate.opsForValue().get(STOCK_KEY);
        assertThat(successCount.get()).isEqualTo(stock);
        assertThat(remaining).isEqualTo("0");
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 차감 실패")
    void decreaseNotEnoughStock() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "2");

        boolean result = stockRepository.decrease(EVENT_ID, 3);

        assertThat(result).isFalse();
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("2");
    }

    @Test
    @DisplayName("수량이 0 이하면 차감 실패")
    void decreaseInvalidQuantity() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "10");

        boolean result = stockRepository.decrease(EVENT_ID, 0);

        assertThat(result).isFalse();
        assertThat(stringRedisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("10");
    }
}
