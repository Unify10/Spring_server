package com.unify.onyak.domain.users.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SettingEditServiceImplTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    @Autowired private ObjectMapper objectMapper;   // 스프링 설정(SNAKE_CASE)이 적용된 매퍼
    private User testUser;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.createLocal(
                "test1234@test.com",
                "temp-hash",
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

        userRepository.save(testUser);
        testUserDetails = new CustomUserDetails(
                testUser.getId(),
                testUser.getName(),
                testUser.getPasswordHash(),
                "ROLE_USER"
        );
    }

    @Test
    @DisplayName("환경설정 변경 : true, false, false")
    void EditSetting() throws Exception {

        SettingEditRequest request = new SettingEditRequest(
                true,
                false,
                false
        );

        mockMvc.perform(patch("/users/me/settings")
                    .with(csrf())
                    .with(user(testUserDetails))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(testUser.getId())
                .orElseThrow(() -> new AssertionError("유저를 찾을 수 없습니다."));

        assertThat(updatedUser.getMedicationPushEnabled()).isTrue();
        assertThat(updatedUser.getSosPushEnabled()).isFalse();
        assertThat(updatedUser.getMarketingPushEnabled()).isFalse();
    }
}