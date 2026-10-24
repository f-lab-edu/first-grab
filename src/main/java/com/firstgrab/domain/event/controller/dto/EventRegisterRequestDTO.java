package com.firstgrab.domain.event.controller.dto;

import com.firstgrab.domain.event.service.dto.EventRegisterCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventRegisterRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Positive
    private long price;

    @Positive
    private int totalStock;

    @Positive
    private int perUserLimit;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    public EventRegisterCommand toCommand() {
        return new EventRegisterCommand(name, price, totalStock, perUserLimit, startAt, endAt);
    }
}
