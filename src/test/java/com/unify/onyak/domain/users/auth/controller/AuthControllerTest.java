package com.unify.onyak.domain.users.auth.controller;

import com.unify.onyak.domain.users.auth.dto.request.SignUpRequest;
import com.unify.onyak.domain.users.auth.entity.EmailVerification;
import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.EmailVerificationRepository;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailVerificationRepository emailVerificationRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private ObjectMapper objectMapper;   // 스프링 설정(SNAKE_CASE)이 적용된 매퍼


    @Test
    @DisplayName("푸시 알림 필드가 누락되어 가입 요청을 보내도 기본값 true로 회원가입에 성공")
    void signUpWithMissingPushFields() throws Exception {

        String testEmail = "test1234@test.com";

        EmailVerification verification = new EmailVerification(testEmail, UUID.randomUUID().toString(), true);
        emailVerificationRepository.save(verification);

        // medication, sos, marketing를 의도적으로 제외
        SignUpRequest signUpRequest = new SignUpRequest(
                testEmail,
                "password1234",
                "password1234",
                "홍길동",
                "010-1234-5678",
                LocalDate.of(2000, 1, 1),
                60.5f,
                170.0f,
                Gender.M,
                null, // medicationPushEnabled
                null, // sosPushEnabled
                null  // marketingPushEnabled
        );

        String body = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(testEmail))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andExpect(jsonPath("$.data.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(body).get("data").get("refresh_token").asString();

        // then: DB에 저장된 유저를 꺼내와서 푸시 동의 값이 true인지 검증
        User savedUser = userRepository.findByEmail("test1234@test.com")
                .orElseThrow(() -> new AssertionError("유저가 저장되지 않았습니다."));

        assertThat(savedUser.getMedicationPushEnabled()).isTrue();
        assertThat(savedUser.getSosPushEnabled()).isTrue();
        assertThat(savedUser.getMarketingPushEnabled()).isTrue();

        // 가입 직후 발급한 refresh 토큰이 Redis(refresh:GUARDIAN:{id})에 저장됐는지
        String key = RefreshToken.keyOf(Role.GUARDIAN, savedUser.getId());
        try {
            assertThat(refreshTokenRepository.findById(key))
                    .get()
                    .extracting(RefreshToken::getRefreshToken)
                    .isEqualTo(refreshToken);
        } finally {
            refreshTokenRepository.deleteById(key);   // Redis 는 @Transactional 롤백 대상이 아니라 직접 정리
        }
    }
}