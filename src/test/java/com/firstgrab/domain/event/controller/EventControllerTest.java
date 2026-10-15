package com.firstgrab.domain.event.controller;

import com.firstgrab.domain.event.controller.dto.EventRegisterRequestDTO;
import com.firstgrab.domain.event.service.EventService;
import com.firstgrab.domain.event.service.dto.EventRegisterCommand;
import com.firstgrab.domain.user.entity.Role;
import com.firstgrab.global.config.SecurityConfig;
import com.firstgrab.global.jwt.JwtProvider;
import com.firstgrab.global.security.CustomAccessDeniedHandler;
import com.firstgrab.global.security.CustomAuthenticationEntryPoint;
import com.firstgrab.support.WithMockUserId;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static com.firstgrab.global.exception.ErrorMessage.FORBIDDEN;
import static com.firstgrab.global.response.ApiResponse.EVENT_REGISTER_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(EventController.class)
@Import({SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class})
class EventControllerTest {

    private static final Long EVENT_ID = 1L;
    private static final String NAME = "TEST";
    private static final long PRICE = 3000L;
    private static final int TOTAL_STOCK = 100;
    private static final int PER_USER_LIMIT = 2;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 19, 0);
    private static final LocalDateTime START_AT = NOW.plusHours(1);
    private static final LocalDateTime END_AT = NOW.plusHours(2);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EventService eventService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @WithMockUserId(role = Role.ADMIN)
    @DisplayName("이벤트 등록 성공")
    void registerSuccess() throws Exception {
        EventRegisterRequestDTO eventRegisterRequestDTO =
                new EventRegisterRequestDTO(NAME, PRICE, TOTAL_STOCK, PER_USER_LIMIT, START_AT, END_AT);
        String json = new ObjectMapper().writeValueAsString(eventRegisterRequestDTO);
        when(eventService.register(any(EventRegisterCommand.class))).thenReturn(EVENT_ID);


        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(EVENT_REGISTER_SUCCESS))
                .andExpect(jsonPath("$.data.eventId").value(EVENT_ID));
    }

    @Test
    @WithMockUserId(role = Role.ADMIN)
    @DisplayName("총 재고가 0 이하면 400")
    void registerInvalidRequest() throws Exception {
        EventRegisterRequestDTO eventRegisterRequestDTO =
                new EventRegisterRequestDTO(NAME, PRICE, 0, PER_USER_LIMIT, START_AT, END_AT);
        String json = new ObjectMapper().writeValueAsString(eventRegisterRequestDTO);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must be greater than 0"));

        verifyNoInteractions(eventService);
    }

    @Test
    @WithMockUserId
    @DisplayName("관리자가 아니면 403")
    void registerForbidden() throws Exception {
        EventRegisterRequestDTO eventRegisterRequestDTO =
                new EventRegisterRequestDTO(NAME, PRICE, TOTAL_STOCK, PER_USER_LIMIT, START_AT, END_AT);
        String json = new ObjectMapper().writeValueAsString(eventRegisterRequestDTO);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(FORBIDDEN));

        verifyNoInteractions(eventService);
    }
}