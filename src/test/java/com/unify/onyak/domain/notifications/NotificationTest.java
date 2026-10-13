package com.unify.onyak.domain.notifications;

import com.unify.onyak.domain.notifications.controller.NotificationController;
import com.unify.onyak.domain.notifications.dto.response.NotificationResponse;
import com.unify.onyak.domain.notifications.entity.NotificationType;
import com.unify.onyak.domain.notifications.entity.RecipientType;
import com.unify.onyak.domain.notifications.service.NotificationService;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtProvider;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class NotificationTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean NotificationService notificationService;

    @Test
    void 미인증_요청은_거부된다() throws Exception {
        int status = mockMvc.perform(get("/notifications"))
                .andReturn().getResponse().getStatus();
        assertTrue(status == 401 || status == 403, "status=" + status);
        verifyNoInteractions(notificationService);
    }

    @Test
    void 인증된_보호자는_본인_알림_목록을_조회한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        when(notificationService.getNotifications(3L, RecipientType.GUARDIAN)).thenReturn(List.of(
                new NotificationResponse(1L, NotificationType.SOS, "철수님의 긴급 SOS 요청이 발생했습니다.",
                        37.5, 127.0, false, OffsetDateTime.now())
        ));

        mockMvc.perform(get("/notifications")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("SOS"))
                .andExpect(jsonPath("$.data[0].is_read").value(false));

        verify(notificationService).getNotifications(3L, RecipientType.GUARDIAN);
    }

    @Test
    void 알수없는_역할이면_403을_반환한다() throws Exception {
        CustomUserDetails unknown = new CustomUserDetails(3L, "a@a.com", null, "ROLE_ADMIN");

        mockMvc.perform(get("/notifications")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                unknown, null, unknown.getAuthorities()))))
                .andExpect(status().isForbidden());
    }
}