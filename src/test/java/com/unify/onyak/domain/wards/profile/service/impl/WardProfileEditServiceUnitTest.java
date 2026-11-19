package com.unify.onyak.domain.wards.profile.service.impl;

import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.profile.dto.request.WardProfileEditRequest;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WardProfileEditServiceUnitTest {

    @Mock WardAccessValidator wardAccessValidator;
    @Mock Ward ward;
    @InjectMocks WardProfileEditServiceImpl service;

    @Test
    void partialUpdateReturnsCurrentWardValuesAndUsesGuardianOnlyAccess() {
        CustomUserDetails guardian = new CustomUserDetails(10L, "guardian", "hash", "ROLE_GUARDIAN");
        when(wardAccessValidator.validate(guardian, 42L, Access.GUARDIAN_ONLY)).thenReturn(ward);
        when(ward.getId()).thenReturn(42L);
        when(ward.getName()).thenReturn("영희");
        when(ward.getWeight()).thenReturn(52.5f);
        when(ward.getHeight()).thenReturn(163.0f);
        when(ward.getGender()).thenReturn(Gender.F);
        when(ward.getIsPregnant()).thenReturn(false);

        var response = service.editWardProfile(42L, guardian,
                new WardProfileEditRequest("영희", null, null, null, null));

        verify(ward).updateInformation("영희", null, null, null, null);
        assertThat(response.name()).isEqualTo("영희");
        assertThat(response.weight()).isEqualTo(52.5f);
        assertThat(response.height()).isEqualTo(163.0f);
        assertThat(response.gender()).isEqualTo("F");
        assertThat(response.isPregnant()).isFalse();
    }
}
