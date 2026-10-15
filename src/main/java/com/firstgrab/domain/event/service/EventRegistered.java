package com.firstgrab.domain.event.service;

import java.time.LocalDateTime;

public record EventRegistered(Long eventId, int totalStock, LocalDateTime endAt) {
}
