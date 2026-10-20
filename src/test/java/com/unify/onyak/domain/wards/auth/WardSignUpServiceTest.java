package com.unify.onyak.domain.wards.auth;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.UserIdNotFoundException;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.wards.auth.dto.request.WardSignUpRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.error.exception.GuardianRoleRequiredException;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.auth.service.WardSignUpService;
import com.unify.onyak.domain.wards.auth.service.impl.WardSignUpServiceImpl;
import com.unify.onyak.domain.wards.link_codes.service.LinkCodeService;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WardSignUpServiceTest {

    private UserRepository userRepository;
    private WardRepository wardRepository;
    private WardSignUpService wardSignUpService;
    private LinkCodeService linkCodeService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        wardRepository = mock(WardRepository.class);
        wardSignUpService = new WardSignUpServiceImpl(userRepository, wardRepository, linkCodeService);
    }

    @Test
    void 보호자가_피보호자를_정상_가입시킨다() {
        CustomUserDetails guardianDetails = new CustomUserDetails(1L, "guardian", null, "ROLE_GUARDIAN");
        User guardian = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(guardian));

        WardSignUpRequest request = validRequest();
        WardSignUpResponse response = wardSignUpService.signUp(guardianDetails, request);

        verify(userRepository).findById(1L);
        verify(wardRepository).save(any(Ward.class));
        assertEquals("피보호자", response.name());
    }

    @Test
    void 보호자가_아닌_role은_가입할_수_없다() {
        CustomUserDetails wardDetails = new CustomUserDetails(2L, "ward", null, "ROLE_WARD");

        assertThrows(GuardianRoleRequiredException.class,
                () -> wardSignUpService.signUp(wardDetails, validRequest()));

        verifyNoInteractions(userRepository, wardRepository);
    }

    @Test
    void 보호자_사용자가_없으면_예외를_던진다() {
        CustomUserDetails guardianDetails = new CustomUserDetails(999L, "guardian", null, "ROLE_GUARDIAN");
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserIdNotFoundException.class,
                () -> wardSignUpService.signUp(guardianDetails, validRequest()));

        verifyNoInteractions(wardRepository);
    }

    private WardSignUpRequest validRequest() {
        return new WardSignUpRequest(
                "피보호자",
                new Date(),
                50.0f,
                160.0f,
                Gender.F,
                false,
                true,
                true
        );
    }
}
