package com.unify.onyak.domain.users.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unify.onyak.domain.users.auth.dto.request.SignUpRequest;
import com.unify.onyak.domain.users.auth.entity.EmailVerification;
import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.EmailVerificationRepository;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailVerificationRepository emailVerificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk());

        // then: DB에 저장된 유저를 꺼내와서 푸시 동의 값이 true인지 검증
        User savedUser = userRepository.findByEmail("test1234@test.com")
                .orElseThrow(() -> new AssertionError("유저가 저장되지 않았습니다."));

        assertThat(savedUser.getMedicationPushEnabled()).isTrue();
        assertThat(savedUser.getSosPushEnabled()).isTrue();
        assertThat(savedUser.getMarketingPushEnabled()).isTrue();
    }
}