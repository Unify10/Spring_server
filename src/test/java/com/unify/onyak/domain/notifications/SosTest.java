package com.unify.onyak.domain.notifications;

import com.unify.onyak.domain.notifications.controller.SosController;
import com.unify.onyak.domain.notifications.error.exception.SosForbiddenException;
import com.unify.onyak.domain.notifications.service.SosService;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtProvider;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.unify.onyak.domain.notifications.dto.request.SosRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SosController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class SosTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean SosService sosService;

    @Test
    void 미인증_요청은_거부된다() throws Exception {
        int status = mockMvc.perform(post("/sos")
                        .contentType("application/json")
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertEquals(401, status);
        verifyNoInteractions(sosService);
    }

    @Test
    void 인증된_피보호자는_SOS를_전송한다() throws Exception {
        CustomUserDetails ward = new CustomUserDetails(7L, "code", null, "ROLE_WARD");

        mockMvc.perform(post("/sos")
                        .with(authentication(new UsernamePasswordAuthenticationToken(ward, null, ward.getAuthorities())))
                        .contentType("application/json")
                        .content("{\"locationLat\":37.5,\"locationLng\":127.0}"))
                .andExpect(status().isOk());

        verify(sosService).sendSos(eq(7L), eq(Role.WARD), any(SosRequest.class));
    }

    @Test
    void 위치정보_없이도_SOS를_전송할_수_있다() throws Exception {
        CustomUserDetails ward = new CustomUserDetails(9L, "code", null, "ROLE_WARD");

        mockMvc.perform(post("/sos")
                        .with(authentication(new UsernamePasswordAuthenticationToken(ward, null, ward.getAuthorities())))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        verify(sosService).sendSos(eq(9L), eq(Role.WARD), any(SosRequest.class));
    }

    @Test
    void 보호자는_SOS를_보낼_수_없어_403을_반환한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        doThrow(new SosForbiddenException()).when(sosService)
                .sendSos(eq(3L), eq(Role.GUARDIAN), any(SosRequest.class));

        mockMvc.perform(post("/sos")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities())))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
