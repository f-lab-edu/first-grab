package com.firstgrab.domain.queue.controller.dto;

import com.firstgrab.domain.queue.service.dto.QueueEntryResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QueueEntryResponseDTO {

    private final long position;
    private final int pollIntervalSeconds;

    public static QueueEntryResponseDTO from(QueueEntryResult queueEntryResult) {
        return new QueueEntryResponseDTO(queueEntryResult.getPosition(), queueEntryResult.getPollIntervalSeconds());
    }
}
