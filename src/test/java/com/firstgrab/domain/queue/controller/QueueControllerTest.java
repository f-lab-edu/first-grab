package com.firstgrab.domain.queue.controller;

import com.firstgrab.domain.queue.service.QueueService;
import com.firstgrab.domain.queue.service.dto.QueueEntryResult;
import com.firstgrab.global.config.SecurityConfig;
import com.firstgrab.global.exception.BadRequestException;
import com.firstgrab.global.exception.NotFoundException;
import com.firstgrab.global.jwt.JwtProvider;
import com.firstgrab.global.security.CustomAccessDeniedHandler;
import com.firstgrab.global.security.CustomAuthenticationEntryPoint;
import com.firstgrab.support.WithMockUserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.firstgrab.global.exception.ErrorMessage.EVENT_NOT_FOUND;
import static com.firstgrab.global.exception.ErrorMessage.EVENT_NOT_OPEN;
import static com.firstgrab.global.exception.ErrorMessage.NOT_IN_QUEUE;
import static com.firstgrab.global.exception.ErrorMessage.UNAUTHORIZED_ACCESS;
import static com.firstgrab.global.response.ApiResponse.QUEUE_ENTER_SUCCESS;
import static com.firstgrab.global.response.ApiResponse.QUEUE_POSITION_SUCCESS;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueueController.class)
@Import({SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class})
public class QueueControllerTest {

    private static final Long EVENT_ID = 1L;
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueService queueService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @WithMockUserId
    @DisplayName("대기열 진입 성공")
    void enterSuccess() throws Exception {
        when(queueService.enter(EVENT_ID, USER_ID))
                .thenReturn(new QueueEntryResult(1L, 1));

        mockMvc.perform(post("/api/events/{eventId}/queue", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(QUEUE_ENTER_SUCCESS))
                .andExpect(jsonPath("$.data.position").value(1))
                .andExpect(jsonPath("$.data.pollIntervalSeconds").value(1));
    }

    @Test
    @DisplayName("로그인하지 않으면 401")
    void enterUnauthorized() throws Exception {
        mockMvc.perform(post("/api/events/{eventId}/queue", EVENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(UNAUTHORIZED_ACCESS));

        verifyNoInteractions(queueService);
    }

    @Test
    @WithMockUserId
    @DisplayName("존재하지 않는 이벤트면 404")
    void enterEventNotFound() throws Exception {
        when(queueService.enter(EVENT_ID, USER_ID))
                .thenThrow(new NotFoundException(EVENT_NOT_FOUND));

        mockMvc.perform(post("/api/events/{eventId}/queue", EVENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(EVENT_NOT_FOUND));
    }

    @Test
    @WithMockUserId
    @DisplayName("열리지 않은 이벤트면 400")
    void enterEventNotOpen() throws Exception {
        when(queueService.enter(EVENT_ID, USER_ID))
                .thenThrow(new BadRequestException(EVENT_NOT_OPEN));

        mockMvc.perform(post("/api/events/{eventId}/queue", EVENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(EVENT_NOT_OPEN));
    }

    @Test
    @WithMockUserId
    @DisplayName("대기 순번 조회 성공")
    void getPositionSuccess() throws Exception {
        when(queueService.getPosition(EVENT_ID, USER_ID))
                .thenReturn(new QueueEntryResult(1L, 1));

        mockMvc.perform(get("/api/events/{eventId}/queue", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(QUEUE_POSITION_SUCCESS))
                .andExpect(jsonPath("$.data.position").value(1))
                .andExpect(jsonPath("$.data.pollIntervalSeconds").value(1));
    }

    @Test
    @WithMockUserId
    @DisplayName("대기열에 없으면 404")
    void getPositionNotInQueue() throws Exception {
        when(queueService.getPosition(EVENT_ID, USER_ID))
                .thenThrow(new NotFoundException(NOT_IN_QUEUE));

        mockMvc.perform(get("/api/events/{eventId}/queue", EVENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(NOT_IN_QUEUE));
    }
}
