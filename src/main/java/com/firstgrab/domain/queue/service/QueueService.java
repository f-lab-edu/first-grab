package com.firstgrab.domain.queue.service;

import com.firstgrab.domain.event.entity.Event;
import com.firstgrab.domain.event.repository.EventRepository;
import com.firstgrab.domain.queue.repository.QueueRepository;
import com.firstgrab.domain.queue.service.dto.QueueEntryResult;
import com.firstgrab.global.exception.BadRequestException;
import com.firstgrab.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import static com.firstgrab.global.exception.ErrorMessage.EVENT_NOT_FOUND;
import static com.firstgrab.global.exception.ErrorMessage.EVENT_NOT_OPEN;

@Slf4j
@RequiredArgsConstructor
@Service
public class QueueService {

    private final Clock clock;
    private final EventRepository eventRepository;
    private final QueueRepository queueRepository;

    public QueueEntryResult enter(Long eventId, Long userId) {
        Instant now = Instant.now(clock);

        Event event = findEvent(eventId);
        validateOpen(event, LocalDateTime.ofInstant(now, clock.getZone()));

        queueRepository.addIfAbsent(eventId, userId, now.toEpochMilli());
        long position = findPosition(eventId, userId);
        int pollIntervalSeconds = calculatePollInterval(position);

        log.debug("User entered queue, eventId={}, userId={}, position={}", eventId, userId, position);
        return new QueueEntryResult(position, pollIntervalSeconds);
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND));
    }

    private void validateOpen(Event event, LocalDateTime nowDateTime) {
        if (!event.isOpen(nowDateTime)) {
            throw new BadRequestException(EVENT_NOT_OPEN);
        }
    }

    private long findPosition(Long eventId, Long userId) {
        long rank = queueRepository.findRank(eventId, userId)
                .orElseThrow(() -> new IllegalStateException(
                        "대기열 진입 직후 순번을 찾을 수 없습니다. eventId=" + eventId + ", userId=" + userId));
        return rank + 1;
    }

    private int calculatePollInterval(long position) {
        if (position <= 10L) {
            return 1;
        }
        if (position <= 100L) {
            return 3;
        }
        if (position <= 1000L) {
            return 5;
        }
        return 10;
    }
}
