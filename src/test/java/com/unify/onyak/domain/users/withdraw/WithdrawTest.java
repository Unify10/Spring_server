package com.unify.onyak.domain.users.withdraw;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.withdraw.controller.WithdrawController;
import com.unify.onyak.domain.users.withdraw.error.exception.WithdrawForbiddenException;
import com.unify.onyak.domain.users.withdraw.service.WithdrawService;
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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WithdrawController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class WithdrawTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean WithdrawService withdrawService;

    @Test
    void 미인증_요청은_거부된다() throws Exception {
        mockMvc.perform(delete("/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(withdrawService);
    }

    @Test
    void 인증된_보호자는_회원_탈퇴한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");

        mockMvc.perform(delete("/users/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isOk());

        verify(withdrawService).withdraw(3L, Role.GUARDIAN);
    }

    @Test
    void 피보호자는_탈퇴할_수_없어_403을_반환한다() throws Exception {
        CustomUserDetails ward = new CustomUserDetails(3L, "3", null, "ROLE_WARD");
        doThrow(new WithdrawForbiddenException()).when(withdrawService).withdraw(3L, Role.WARD);

        mockMvc.perform(delete("/users/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                ward, null, ward.getAuthorities()))))
                .andExpect(status().isForbidden());
    }
}