package com.firstgrab.domain.event.entity;

import com.firstgrab.global.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.firstgrab.global.exception.ErrorMessage.INVALID_EVENT_PERIOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class EventTest {

    private static final String NAME = "test event";
    private static final long PRICE = 100_000L;
    private static final int TOTAL_STOCK = 100;
    private static final int PER_USER_LIMIT = 1;
    private static final LocalDateTime START_AT = LocalDateTime.of(2026, 10, 10, 10, 0);
    private static final LocalDateTime END_AT = START_AT.plusHours(1);

    @Test
    @DisplayName("이벤트 생성 성공")
    void createEventSuccess() {
        Event event = createEvent();

        assertThat(event.getStartAt()).isEqualTo(START_AT);
        assertThat(event.getEndAt()).isEqualTo(END_AT);
    }

    @Test
    @DisplayName("시작 시각과 종료 시각이 같으면 이벤트 생성 실패")
    void createEventInvalidPeriod() {
        assertThatThrownBy(() -> Event.createEvent(NAME, PRICE, TOTAL_STOCK, PER_USER_LIMIT, START_AT, START_AT))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID_EVENT_PERIOD);
    }

    @Test
    @DisplayName("시작 시각과 같으면 열린 이벤트")
    void isOpenAtStartAt() {
        Event event = createEvent();

        assertThat(event.isOpen(START_AT)).isTrue();
    }

    @Test
    @DisplayName("종료 시각과 같으면 닫힌 이벤트")
    void isOpenAtEndAt() {
        Event event = createEvent();

        assertThat(event.isOpen(END_AT)).isFalse();
    }

    private Event createEvent() {
        return Event.createEvent(NAME, PRICE, TOTAL_STOCK, PER_USER_LIMIT, START_AT, END_AT);
    }
}
