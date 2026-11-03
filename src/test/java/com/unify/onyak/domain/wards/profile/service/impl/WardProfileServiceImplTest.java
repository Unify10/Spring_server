package com.unify.onyak.domain.wards.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.error.exception.GuardianRoleRequiredException;
import com.unify.onyak.domain.wards.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.wards.error.exception.WardNotFoundException;
import com.unify.onyak.domain.wards.profile.dto.response.WardDetailResponse;
import com.unify.onyak.domain.wards.profile.dto.response.WardListResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WardProfileServiceImplTest {

    private WardRepository wardRepository;
    private WardProfileServiceImpl wardProfileService;

    @BeforeEach
    void setUp() {
        wardRepository = mock(WardRepository.class);
        wardProfileService = new WardProfileServiceImpl(wardRepository);
    }

    @Test
    void 보호자의_피보호자_목록을_조회한다() {
        User guardian = guardian(1L);
        Ward ward = ward(10L, guardian, "홍길동");
        CustomUserDetails details = guardianDetails(1L);
        when(wardRepository.findByGuardianId_Id(1L)).thenReturn(List.of(ward));

        List<WardListResponse> response = wardProfileService.getWards(details);

        assertEquals(1, response.size());
        assertEquals(10L, response.get(0).wardId());
        assertEquals("홍길동", response.get(0).name());
        verify(wardRepository).findByGuardianId_Id(1L);
    }

    @Test
    void 보호자가_자신의_피보호자_상세정보를_조회한다() {
        User guardian = guardian(1L);
        Ward ward = ward(10L, guardian, "홍길동");
        when(wardRepository.findById(10L)).thenReturn(Optional.of(ward));

        WardDetailResponse response = wardProfileService.getWard(guardianDetails(1L), 10L);

        assertEquals(10L, response.wardId());
        assertEquals("홍길동", response.name());
        verify(wardRepository).findById(10L);
    }

    @Test
    void 보호자가_아니면_피보호자_목록을_조회할_수_없다() {
        CustomUserDetails wardDetails = new CustomUserDetails(10L, "ward", null, "ROLE_WARD");

        assertThrows(GuardianRoleRequiredException.class,
                () -> wardProfileService.getWards(wardDetails));

        verifyNoInteractions(wardRepository);
    }

    @Test
    void 다른_보호자의_피보호자_상세정보를_조회할_수_없다() {
        User owner = guardian(2L);
        Ward ward = ward(10L, owner, "홍길동");
        when(wardRepository.findById(10L)).thenReturn(Optional.of(ward));

        assertThrows(WardAccessDeniedException.class,
                () -> wardProfileService.getWard(guardianDetails(1L), 10L));
    }

    @Test
    void 존재하지_않는_피보호자면_예외가_발생한다() {
        when(wardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(WardNotFoundException.class,
                () -> wardProfileService.getWard(guardianDetails(1L), 999L));
    }

    private CustomUserDetails guardianDetails(Long userId) {
        return new CustomUserDetails(userId, "guardian", null, "ROLE_GUARDIAN");
    }

    private User guardian(Long userId) {
        User guardian = mock(User.class);
        when(guardian.getId()).thenReturn(userId);
        return guardian;
    }

    private Ward ward(Long wardId, User guardian, String name) {
        Ward ward = Ward.createLocal(
                guardian,
                name,
                LocalDate.of(2010, 1, 1),
                50.0f,
                160.0f,
                Gender.F,
                false,
                true,
                true
        );

        return mockWardWithId(ward, wardId);
    }

    private Ward mockWardWithId(Ward ward, Long wardId) {
        Ward spy = spy(ward);
        when(spy.getId()).thenReturn(wardId);
        return spy;
    }
}
