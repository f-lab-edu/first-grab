package com.firstgrab.domain.event.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EventRegisterCommand {

    private final String name;
    private final long price;
    private final int totalStock;
    private final int perUserLimit;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
}
