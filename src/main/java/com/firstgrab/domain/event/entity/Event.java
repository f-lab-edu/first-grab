package com.firstgrab.domain.event.entity;

import com.firstgrab.global.entity.BaseEntity;
import com.firstgrab.global.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.firstgrab.global.exception.ErrorMessage.INVALID_EVENT_PERIOD;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private int totalStock;

    @Column(nullable = false)
    private int perUserLimit;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column
    private LocalDateTime deletedAt;

    private Event(String name, long price, int totalStock,
                  int perUserLimit, LocalDateTime startAt, LocalDateTime endAt) {
        this.name = name;
        this.price = price;
        this.totalStock = totalStock;
        this.perUserLimit = perUserLimit;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static Event createEvent(String name, long price, int totalStock,
                                    int perUserLimit, LocalDateTime startAt, LocalDateTime endAt) {
        validatePeriod(startAt, endAt);
        return new Event(name, price, totalStock, perUserLimit, startAt, endAt);
    }

    private static void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (!startAt.isBefore(endAt)) {
            throw new BadRequestException(INVALID_EVENT_PERIOD);
        }
    }

    public boolean isOpen(LocalDateTime currentAt) {
        return (!currentAt.isBefore(startAt) && currentAt.isBefore(endAt));
    }

}
