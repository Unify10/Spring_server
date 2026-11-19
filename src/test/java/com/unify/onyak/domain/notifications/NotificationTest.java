package com.unify.onyak.domain.notifications;

import com.unify.onyak.domain.notifications.controller.NotificationController;
import com.unify.onyak.domain.notifications.dto.response.NotificationResponse;
import com.unify.onyak.domain.notifications.entity.NotificationType;
import com.unify.onyak.domain.notifications.entity.RecipientType;
import com.unify.onyak.domain.notifications.error.exception.InvalidPageRequestException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(401, status);
        verifyNoInteractions(notificationService);
    }

    @Test
    void 인증된_보호자는_본인_알림_목록을_조회한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        when(notificationService.getNotifications(3L, RecipientType.GUARDIAN, 0, 20)).thenReturn(List.of(
                new NotificationResponse(1L, NotificationType.SOS, "철수님의 긴급 SOS 요청이 발생했습니다.",
                        37.5, 127.0, false, OffsetDateTime.now())
        ));

        mockMvc.perform(get("/notifications")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("SOS"))
                .andExpect(jsonPath("$.data[0].is_read").value(false));

        verify(notificationService).getNotifications(3L, RecipientType.GUARDIAN, 0, 20);
    }

    @Test
    void page와_size를_지정하면_그대로_서비스에_전달한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        when(notificationService.getNotifications(3L, RecipientType.GUARDIAN, 1, 5)).thenReturn(List.of());

        mockMvc.perform(get("/notifications")
                        .param("page", "1")
                        .param("size", "5")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(notificationService).getNotifications(3L, RecipientType.GUARDIAN, 1, 5);
    }

    @Test
    void 잘못된_페이지_요청이면_400을_반환한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        when(notificationService.getNotifications(3L, RecipientType.GUARDIAN, 0, 101))
                .thenThrow(new InvalidPageRequestException());

        mockMvc.perform(get("/notifications")
                        .param("size", "101")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_PAGE_REQUEST"));
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
