package com.firstgrab.domain.event.service;

import com.firstgrab.domain.event.entity.Event;
import com.firstgrab.domain.event.repository.EventRepository;
import com.firstgrab.domain.event.service.dto.EventRegisterCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class EventService {

    private final Clock clock;
    private final EventRepository eventRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public Long register(EventRegisterCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Event event = Event.createEvent(
                command.getName(), command.getPrice(),
                command.getTotalStock(), command.getPerUserLimit(),
                command.getStartAt(), command.getEndAt(), now);

        Event saved = eventRepository.save(event);
        publisher.publishEvent(
                new EventRegistered(saved.getId(), saved.getTotalStock(), saved.getEndAt()));

        return saved.getId();
    }
}
