package com.firstgrab.global.jwt;

import com.firstgrab.domain.user.entity.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "token";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Long USER_ID = 1L;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보 저장")
    void filterValidToken() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + TOKEN);
        when(jwtProvider.parseToken(TOKEN)).thenReturn(Optional.of(new TokenClaims(USER_ID, Role.USER)));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(USER_ID);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증 정보를 저장하지 않음")
    void filterInvalidToken() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + TOKEN);
        when(jwtProvider.parseToken(TOKEN)).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 정보를 저장하지 않음")
    void filterNoHeader() throws Exception {
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verifyNoInteractions(jwtProvider);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 로 시작하지 않으면 인증 정보를 저장하지 않음")
    void filterNoBearer() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + TOKEN);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verifyNoInteractions(jwtProvider);
        verify(filterChain).doFilter(request, response);
    }

}
