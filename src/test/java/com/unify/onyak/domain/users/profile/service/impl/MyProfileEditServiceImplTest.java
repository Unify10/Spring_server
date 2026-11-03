package com.unify.onyak.domain.users.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.profile.dto.request.MyProfileEditRequest;
import com.unify.onyak.domain.users.profile.error.exception.UserNotFound;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MyProfileEditServiceImplTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    private User normalUser;
    private User googleUser;
    private User wardUser;
    private UserDetails normalUserDetails;
    private UserDetails googleUserDetails;
    private UserDetails wardUserDetails;

    @BeforeEach
    void setUp() {
        String normalPassword = passwordEncoder.encode("asdf1234");

        normalUser = User.createLocal(
                "test1234@test.com",
                normalPassword,
                "test",
                "010-0000-0000",
                LocalDate.of(2000, 1, 1),
                60.5f,
                170.0f,
                Gender.M,
                true,
                true,
                true
        );

        googleUser = User.createLocal(
                "test1234@google.com",
                passwordEncoder.encode(UUID.randomUUID().toString()), // null은 할 수 없으니 랜덤으로 못 맞추게 설정
                "test",
                "010-0000-0000",
                LocalDate.of(2000, 1, 1),
                60.5f,
                170.0f,
                Gender.M,
                true,
                true,
                true
        );

        wardUser = User.createLocal(
                "ward1234@test.com",
                normalPassword,
                "ward",
                "010-0000-0000",
                LocalDate.of(2010,1,1),
                60.5f,
                170.5f,
                Gender.M,
                true,
                true,
                true
        );

        userRepository.save(normalUser);
        userRepository.save(googleUser);
        userRepository.save(wardUser);

        normalUserDetails = new CustomUserDetails(
                normalUser.getId(),
                normalUser.getName(),
                normalUser.getPasswordHash(),
                "ROLE_GUARDIAN"
        );
        googleUserDetails = new CustomUserDetails(
                googleUser.getId(),
                googleUser.getName(),
                googleUser.getPasswordHash(),
                "ROLE_GUARDIAN"
        );
        wardUserDetails = new CustomUserDetails(
                wardUser.getId(),
                wardUser.getName(),
                wardUser.getPasswordHash(),
                "ROLE_WARD"
        );
    }

    @Test
    @DisplayName("일반 사용자는 encode와 matches흐름을 통해 정보 수정 성공")
    void updateProfile_Normal() throws Exception {
        MyProfileEditRequest request = new MyProfileEditRequest(
                "010-1111-1111",
                "qwer1234",
                "asdf1234"
        );

        mockMvc.perform(patch("/users/me")
                        .with(csrf())
                        .with(user(normalUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User updUser = userRepository.findById(normalUser.getId())
                .orElseThrow(UserNotFound::new);

        assertThat(updUser.getPhone()).isEqualTo("010-1111-1111");
        assertThat(passwordEncoder.matches("qwer1234", updUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("보호자가 아닌 사용자는 수정 시도 시 403을 던진다")
    void updateProfile_forbidden() throws Exception {
        MyProfileEditRequest request = new MyProfileEditRequest(
                "010-1111-1111",
                "qwer1234",
                "asdf1234"
        );

        mockMvc.perform(patch("/users/me")
                .with(csrf())
                .with(user(wardUserDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("구글 사용자가 비밀번호 수정 시 비밀번호 불일치로 400을 던진다")
    void updateProfile_google() throws Exception {
        MyProfileEditRequest request = new MyProfileEditRequest(
                "010-1111-1111",
                "qwer1234",
                "asdf1234"
        );

        mockMvc.perform(patch("/users/me")
                .with(csrf())
                .with(user(googleUserDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구글 사용자가 비밀번호, 현제 비밀번호에 null 입력 시 핸드폰 번호만 변경된다")
    void updateProfile_null() throws Exception {
        MyProfileEditRequest request = new MyProfileEditRequest(
                "010-1111-1111",
                null,
                null
        );

        mockMvc.perform(patch("/users/me")
                .with(csrf())
                .with(user(googleUserDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        User updUser = userRepository.findById(googleUser.getId())
                .orElseThrow(UserNotFound::new);

        assertThat(updUser.getPhone()).isEqualTo("010-1111-1111"); // 비밀번호는 랜덤 해시이므로 확인 불가
    }
}