package com.unify.onyak.domain.wards.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.profile.dto.request.WardProfileEditRequest;
import com.unify.onyak.domain.wards.profile.error.exception.WardNotFound;
import com.unify.onyak.global.security.CustomUserDetails;
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
class WardProfileEditServiceImplTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WardRepository wardRepository;
    @Autowired private UserRepository userRepository;

    private UserDetails guardianDetails;
    private UserDetails wardDetails;

    private User guardian() {
        User guardian = User.createLocal(
                "guardian@test.com",
                "tempHash",
                "guardian",
                "010-0000-0000",
                LocalDate.of(2000, 1, 1),
                60.5f,
                170.0f,
                Gender.M,
                true,
                true,
                true
        );

        userRepository.save(guardian);

        guardianDetails = new CustomUserDetails(
                guardian.getId(),
                "guardian",
                "tempHash",
                "ROLE_GUARDIAN"
        );

        return guardian;
    }

    private Ward ward(User guardian) {
        Ward ward = Ward.createLocal(
                guardian,
                "ward",
                LocalDate.of(2010, 1, 1),
                50.0f,
                160.0f,
                Gender.F,
                false,
                true,
                true
        );

        wardRepository.save(ward);

        wardDetails = new CustomUserDetails(
                ward.getId(),
                ward.getName(),
                "tempHash",
                "ROLE_WARD"
        );

        return ward;
    }

    @Test
    @DisplayName("보호자가 피보호자의 정보를 정상적으로 수정한다.")
    void editWardProfile() throws Exception {
        User guardian = guardian();
        Ward ward = ward(guardian);
        WardProfileEditRequest request = new WardProfileEditRequest(
                "Changed",
                70.3f,
                175.3f,
                ward.getGender().toString(),
                ward.getIsPregnant()
        );

        mockMvc.perform(patch("/wards/" +  ward.getId())
                .with(csrf())
                .with(user(guardianDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        Ward changedWard = wardRepository.findById(ward.getId())
                .orElseThrow(WardNotFound::new);
        assertThat(changedWard.getName()).isEqualTo("Changed");
        assertThat(changedWard.getWeight()).isEqualTo(70.3f);
        assertThat(changedWard.getHeight()).isEqualTo(175.3f);
    }

    @Test
    @DisplayName("보호자가 본인의 피보호자가 아닌 id로 호출 시 오류")
    void editWardProfileId() throws Exception {
        User guardian = guardian();
        Ward ward = ward(guardian);

        WardProfileEditRequest request = new WardProfileEditRequest(
                "Changed",
                70.3f,
                175.3f,
                ward.getGender().toString(),
                ward.getIsPregnant()
        );

        mockMvc.perform(patch("/wards/" + ward.getId() + 1)
                        .with(csrf())
                        .with(user(guardianDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("피보호자가 피보호자의 프로필 수정 시 401")
    void editWardProfileWrongId() throws Exception {
        User guardian = guardian();
        Ward ward = ward(guardian);

        WardProfileEditRequest request = new WardProfileEditRequest(
                "Changed",
                70.3f,
                175.3f,
                ward.getGender().toString(),
                ward.getIsPregnant()
        );

        mockMvc.perform(patch("/wards/" + ward.getId())
                        .with(csrf())
                        .with(user(wardDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
