package com.firstgrab.domain.queue.repository;

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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import({RedisContainerConfig.class, QueueRepository.class})
public class QueueRepositoryTest {

    private static final Long EVENT_ID = 1L;
    private static final Instant EXPIRE_AT = Instant.now().plus(Duration.ofHours(1));

    @Autowired
    private QueueRepository queueRepository;

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
    @DisplayName("이미 진입한 사용자가 다시 진입하면 순번 유지")
    void addIfAbsentAlreadyEntered() {
        queueRepository.addIfAbsent(EVENT_ID, 1L, 1000L, EXPIRE_AT);
        queueRepository.addIfAbsent(EVENT_ID, 2L, 2000L, EXPIRE_AT);

        queueRepository.addIfAbsent(EVENT_ID, 1L, 3000L, EXPIRE_AT);

        assertThat(queueRepository.findRank(EVENT_ID, 1L)).contains(0L);
    }

    @Test
    @DisplayName("대기열에 없으면 empty를 반환")
    void findRankNotInQueue() {
        assertThat(queueRepository.findRank(EVENT_ID, 1L)).isEmpty();
    }

    @Test
    @DisplayName("진입하면 대기열 키에 만료 시각 설정")
    void addIfAbsentSetExpireAt() {
        queueRepository.addIfAbsent(EVENT_ID, 1L, 1000L, EXPIRE_AT);

        Long ttl = stringRedisTemplate.getExpire("queue:{" + EVENT_ID + "}");
        assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofHours(1).toSeconds());
    }
}
