package com.firstgrab.domain.stock.listener;

import com.firstgrab.domain.event.service.EventRegistered;
import com.firstgrab.domain.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockInitListenerTest {

    private static final Long EVENT_ID = 1L;
    private static int TOTAL_STOCK = 100;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 19, 30);
    private static final Instant NOW_INSTANT = NOW.atZone(ZONE).toInstant();
    private static final LocalDateTime END_AT = NOW.plusHours(2);
    private static final Duration EXPECTED_TTL = Duration.ofHours(3);

    @Mock
    private StockRepository stockRepository;

    private StockInitListener stockInitListener;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW_INSTANT, ZONE);
        stockInitListener = new StockInitListener(clock, stockRepository);
    }

    @Test
    @DisplayName("재고 초기화 성공")
    void initStockSuccess() {
        when(stockRepository.init(EVENT_ID, TOTAL_STOCK, EXPECTED_TTL)).thenReturn(true);

        stockInitListener.initStock(new EventRegistered(EVENT_ID, TOTAL_STOCK, END_AT));

        verify(stockRepository).init(EVENT_ID, TOTAL_STOCK, EXPECTED_TTL);
    }

    @Test
    @DisplayName("재고 키가 이미 있어도 예외 없이 종료")
    void initStockAlreadyExists() {
        when(stockRepository.init(EVENT_ID, TOTAL_STOCK, EXPECTED_TTL)).thenReturn(false);

        assertThatCode(() -> stockInitListener.initStock(new EventRegistered(EVENT_ID, TOTAL_STOCK, END_AT)))
                .doesNotThrowAnyException();
    }
}
