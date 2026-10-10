package com.firstgrab.domain.queue.service;

import com.firstgrab.domain.event.entity.Event;
import com.firstgrab.domain.event.repository.EventRepository;
import com.firstgrab.domain.queue.repository.QueueRepository;
import com.firstgrab.domain.queue.service.dto.QueueEntryResult;
import com.firstgrab.global.exception.BadRequestException;
import com.firstgrab.global.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static com.firstgrab.global.exception.ErrorMessage.EVENT_NOT_FOUND;
import static com.firstgrab.global.exception.ErrorMessage.EVENT_NOT_OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class QueueServiceTest {

    private static final Long EVENT_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 10, 30);
    private static final Instant NOW_INSTANT = NOW.atZone(ZONE).toInstant();

    @Mock
    private EventRepository eventRepository;

    @Mock
    private QueueRepository queueRepository;

    private QueueService queueService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW_INSTANT, ZONE);
        queueService = new QueueService(clock, eventRepository, queueRepository);
    }

    @Test
    @DisplayName("대기열 진입 성공")
    void enterSuccess() {
        Event event = openEvent();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(queueRepository.findRank(EVENT_ID, USER_ID)).thenReturn(Optional.of(0L));

        QueueEntryResult result = queueService.enter(EVENT_ID, USER_ID);

        assertThat(result.getPosition()).isEqualTo(1L);
        assertThat(result.getPollIntervalSeconds()).isEqualTo(1);

        verify(queueRepository).addIfAbsent(EVENT_ID, USER_ID, NOW_INSTANT.toEpochMilli());
    }

    @Test
    @DisplayName("존재하지 않는 이벤트면 진입 실패")
    void enterEventNotFound() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.enter(EVENT_ID, USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(EVENT_NOT_FOUND);

        verifyNoInteractions(queueRepository);
    }

    @Test
    @DisplayName("열리지 않은 이벤트면 진입 실패")
    void enterEventNotOpen() {
        Event event = notStartedEvent();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> queueService.enter(EVENT_ID, USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(EVENT_NOT_OPEN);

        verifyNoInteractions(queueRepository);
    }

    private Event openEvent() {
        return Event.createEvent("test event", 100_000L, 100, 1,
                NOW.minusMinutes(30), NOW.plusMinutes(30));
    }

    private Event notStartedEvent() {
        return Event.createEvent("test event", 100_000L, 100, 1,
                NOW.plusHours(1), NOW.plusHours(2));
    }
}
