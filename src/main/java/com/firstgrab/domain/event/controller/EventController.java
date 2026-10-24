package com.firstgrab.domain.event.controller;

import com.firstgrab.domain.event.controller.dto.EventRegisterRequestDTO;
import com.firstgrab.domain.event.controller.dto.EventRegisterResponseDTO;
import com.firstgrab.domain.event.service.EventService;
import com.firstgrab.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ApiResponse<EventRegisterResponseDTO>> register(
            @Valid @RequestBody EventRegisterRequestDTO eventRegisterRequestDTO) {
        Long eventId = eventService.register(eventRegisterRequestDTO.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value(), ApiResponse.EVENT_REGISTER_SUCCESS,
                        EventRegisterResponseDTO.from(eventId)));
    }
}
