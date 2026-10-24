package com.firstgrab.domain.event.service;

import com.firstgrab.domain.event.entity.Event;
import com.firstgrab.domain.event.repository.EventRepository;
import com.firstgrab.domain.event.service.dto.EventRegisterCommand;
import com.firstgrab.global.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.firstgrab.global.exception.ErrorMessage.INVALID_EVENT_START_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 17, 0);
    private static final Instant NOW_INSTANT = NOW.atZone(ZONE).toInstant();

    private static final Long EVENT_ID = 1L;
    private static final String NAME = "TEST";
    private static final long PRICE = 3000L;
    private static final int TOTAL_STOCK = 100;
    private static final int PER_USER_LIMIT = 2;
    private static final LocalDateTime START_AT = NOW.plusHours(1);
    private static final LocalDateTime END_AT = NOW.plusHours(2);

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW_INSTANT, ZONE);
        eventService = new EventService(clock, eventRepository, publisher);
    }

    @Test
    @DisplayName("이벤트 등록 성공")
    void registerSuccess() {
        EventRegisterCommand command = new EventRegisterCommand(NAME, PRICE, TOTAL_STOCK, PER_USER_LIMIT, START_AT, END_AT);
        Event event = Event.createEvent(NAME, PRICE, TOTAL_STOCK, PER_USER_LIMIT, START_AT, END_AT, NOW);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        Long eventId = eventService.register(command);

        assertThat(eventId).isEqualTo(EVENT_ID);
        verify(publisher).publishEvent(new EventRegistered(EVENT_ID, TOTAL_STOCK, END_AT));
    }

    @Test
    @DisplayName("시작 시각이 현재보다 이전이면 저장과 이벤트 발행 안 함")
    void registerStartAtInPast() {
        EventRegisterCommand command = new EventRegisterCommand(
                NAME, PRICE, TOTAL_STOCK, PER_USER_LIMIT, NOW.minusMinutes(30), END_AT);

        assertThatThrownBy(() -> eventService.register(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID_EVENT_START_AT);

        verifyNoInteractions(eventRepository, publisher);
    }
}
