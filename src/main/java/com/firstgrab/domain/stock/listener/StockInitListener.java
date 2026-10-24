package com.firstgrab.domain.stock.listener;

import com.firstgrab.domain.event.service.EventRegistered;
import com.firstgrab.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockInitListener {

    private static final Duration STOCK_TTL_BUFFER = Duration.ofHours(1);

    private final Clock clock;
    private final StockRepository stockRepository;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void initStock(EventRegistered eventRegistered) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long eventId = eventRegistered.eventId();
        int totalStock = eventRegistered.totalStock();
        Duration ttl = calculateTtl(now, eventRegistered.endAt());

        boolean saved = stockRepository.init(eventId, totalStock, ttl);
        if (!saved) {
            log.warn("재고 키가 이미 있어 초기화할 수 없습니다. eventId={}", eventId);
        }
    }

    private Duration calculateTtl(LocalDateTime currentAt, LocalDateTime endAt) {
        return Duration.between(currentAt, endAt.plus(STOCK_TTL_BUFFER));
    }
}
