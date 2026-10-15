package com.firstgrab.domain.event.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventRegisterResponseDTO {

    private final Long eventId;

    public static EventRegisterResponseDTO from(Long eventId) {
        return new EventRegisterResponseDTO(eventId);
    }
}
