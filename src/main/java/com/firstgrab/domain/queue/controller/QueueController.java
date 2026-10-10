package com.firstgrab.domain.queue.controller;

import com.firstgrab.domain.queue.controller.dto.QueueEntryResponseDTO;
import com.firstgrab.domain.queue.service.QueueService;
import com.firstgrab.domain.queue.service.dto.QueueEntryResult;
import com.firstgrab.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/events/{eventId}/queue")
public class QueueController {

    private final QueueService queueService;

    @PostMapping
    public ResponseEntity<ApiResponse<QueueEntryResponseDTO>> enter(
            @PathVariable Long eventId, @AuthenticationPrincipal Long userId) {
        QueueEntryResult queueEntryResult = queueService.enter(eventId, userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.of(HttpStatus.OK.value(), ApiResponse.QUEUE_ENTER_SUCCESS,
                        QueueEntryResponseDTO.from(queueEntryResult)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<QueueEntryResponseDTO>> getPosition(
            @PathVariable Long eventId, @AuthenticationPrincipal Long userId) {
        QueueEntryResult queueEntryResult = queueService.getPosition(eventId, userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.of(HttpStatus.OK.value(), ApiResponse.QUEUE_POSITION_SUCCESS,
                        QueueEntryResponseDTO.from(queueEntryResult)));
    }
}
