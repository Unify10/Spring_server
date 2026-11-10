package com.unify.onyak.domain.wards.profile;

import com.unify.onyak.domain.wards.profile.controller.WardProfileController;
import com.unify.onyak.domain.wards.profile.dto.response.WardDetailResponse;
import com.unify.onyak.domain.wards.profile.dto.response.WardListResponse;
import com.unify.onyak.domain.wards.profile.service.WardProfileEditService;
import com.unify.onyak.domain.wards.profile.service.WardProfileService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WardProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class WardProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WardProfileService wardProfileService;

    @MockitoBean
    WardProfileEditService wardProfileEditService;

    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    void 인증되지_않은_사용자는_피보호자_목록을_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/wards"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(wardProfileService);
    }

    @Test
    void 보호자는_피보호자_목록을_조회할_수_있다() throws Exception {
        CustomUserDetails guardian = guardianDetails(1L);
        when(wardProfileService.getWards(guardian))
                .thenReturn(List.of(new WardListResponse(10L, "홍길동")));

        mockMvc.perform(get("/wards")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ward_id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("홍길동"));

        verify(wardProfileService).getWards(guardian);
    }

    @Test
    void 보호자는_피보호자_상세정보를_조회할_수_있다() throws Exception {
        CustomUserDetails guardian = guardianDetails(1L);
        when(wardProfileService.getWard(guardian, 10L))
                .thenReturn(new WardDetailResponse(
                        10L,
                        "홍길동",
                        LocalDate.of(2010, 1, 1),
                        50.0f,
                        160.0f,
                        null,
                        false,
                        true,
                        true
                ));

        mockMvc.perform(get("/wards/10")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ward_id").value(10))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.birth_date").value("2010-01-01"));

        verify(wardProfileService).getWard(eq(guardian), eq(10L));
    }

    private CustomUserDetails guardianDetails(Long userId) {
        return new CustomUserDetails(userId, "guardian", null, "ROLE_GUARDIAN");
    }
}
