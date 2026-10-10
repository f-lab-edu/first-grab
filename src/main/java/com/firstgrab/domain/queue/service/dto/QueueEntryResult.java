package com.firstgrab.domain.queue.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QueueEntryResult {

    private final long position;
    private final int pollIntervalSeconds;
}
