package com.unify.onyak.domain.notifications;

import com.unify.onyak.domain.notifications.controller.SosController;
import com.unify.onyak.domain.notifications.service.SosService;
import com.unify.onyak.global.config.SecurityConfig;
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
@Import(SecurityConfig.class)
class SosTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SosService sosService;

    @Test
    void 미인증_요청은_거부된다() throws Exception {
        int status = mockMvc.perform(post("/sos")
                        .contentType("application/json")
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertTrue(status == 401 || status == 403, "status=" + status);
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

        verify(sosService).sendSos(eq(7L), any(SosRequest.class));
    }

    @Test
    void 위치정보_없이도_SOS를_전송할_수_있다() throws Exception {
        CustomUserDetails ward = new CustomUserDetails(9L, "code", null, "ROLE_WARD");

        mockMvc.perform(post("/sos")
                        .with(authentication(new UsernamePasswordAuthenticationToken(ward, null, ward.getAuthorities())))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        verify(sosService).sendSos(eq(9L), any(SosRequest.class));
    }
}
