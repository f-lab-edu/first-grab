package com.firstgrab.domain.user.controller;

import com.firstgrab.domain.user.controller.dto.SignupRequestDTO;
import com.firstgrab.domain.user.service.UserService;
import com.firstgrab.global.exception.DuplicateException;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("회원 가입 성공")
    void signupSuccess() throws Exception {
        SignupRequestDTO signupRequestDTO = new SignupRequestDTO("test@test.com", "12345678", "홍길동");
        String json = new ObjectMapper().writeValueAsString(signupRequestDTO);

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("이메일 중복 시")
    void signupDuplicateEmail() throws Exception {
        SignupRequestDTO signupRequestDTO = new SignupRequestDTO("test@test.com", "12345678", "홍길동");
        String json = new ObjectMapper().writeValueAsString(signupRequestDTO);
        doThrow(new DuplicateException("중복된 이메일입니다."))
                .when(userService).signup(any(SignupRequestDTO.class));

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("중복된 이메일입니다."));
    }

    @Test
    @DisplayName("이메일 미입력 시")
    void signupInvalidEmail() throws Exception {
        SignupRequestDTO signupRequestDTO = new SignupRequestDTO("", "12345678", "홍길동");
        String json = new ObjectMapper().writeValueAsString(signupRequestDTO);

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("must not be blank"));
    }

}
