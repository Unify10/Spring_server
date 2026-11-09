package com.unify.onyak.domain.users.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.profile.dto.request.BodyEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.BodyEditResponse;
import com.unify.onyak.domain.users.profile.error.exception.InvalidPregnancy;
import com.unify.onyak.domain.users.profile.error.exception.UserForbidden;
import com.unify.onyak.domain.users.profile.error.exception.UserNotFound;
import com.unify.onyak.domain.users.profile.service.BodyEditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BodyEditServiceImplTest {

    UserRepository userRepository;
    BodyEditService bodyEditService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        bodyEditService = new BodyEditServiceImpl(userRepository);
    }

    private User user(Gender gender) {
        return User.createLocal(
                "a@a.com", "hash", "테스트", "010-0000-0000",
                LocalDate.of(1990, 1, 1), 60.0f, 170.0f, gender,
                null,
                true, true, true
        );
    }

    @Test
    void 피보호자_role이면_아무것도_조회하지_않고_예외를_던진다() {
        BodyEditRequest request = new BodyEditRequest(null, 50.0f, null, null, null);

        assertThrows(UserForbidden.class,
                () -> bodyEditService.editBody(3L, Role.WARD, request));

        verifyNoInteractions(userRepository);
    }

    @Test
    void 존재하지_않는_사용자면_예외를_던진다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class,
                () -> bodyEditService.editBody(999L, Role.GUARDIAN,
                        new BodyEditRequest(null, 50.0f, null, null, null)));
    }

    @Test
    void 보낸_필드만_수정하고_나머지는_그대로_둔다() {
        User user = user(Gender.F);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        BodyEditResponse response = bodyEditService.editBody(3L, Role.GUARDIAN,
                new BodyEditRequest(null, 55.5f, null, null, null));

        assertThat(user.getWeight()).isEqualTo(55.5f);
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(user.getHeight()).isEqualTo(170.0f);
        assertThat(user.getGender()).isEqualTo(Gender.F);
        assertThat(response.weight()).isEqualTo(55.5f);
        assertThat(response.height()).isEqualTo(170.0f);
    }

    @Test
    void 모든_필드를_수정한다() {
        User user = user(Gender.M);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        BodyEditResponse response = bodyEditService.editBody(3L, Role.GUARDIAN,
                new BodyEditRequest(LocalDate.of(1995, 3, 3), 52.0f, 160.0f, Gender.F, true));

        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 3, 3));
        assertThat(response.weight()).isEqualTo(52.0f);
        assertThat(response.height()).isEqualTo(160.0f);
        assertThat(response.gender()).isEqualTo(Gender.F);
        assertThat(response.isPregnant()).isTrue();
    }

    @Test
    void 남성으로_보내면서_임신이면_예외를_던지고_수정하지_않는다() {
        User user = user(Gender.F);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        assertThrows(InvalidPregnancy.class,
                () -> bodyEditService.editBody(3L, Role.GUARDIAN,
                        new BodyEditRequest(null, 70.0f, null, Gender.M, true)));

        assertThat(user.getGender()).isEqualTo(Gender.F);
        assertThat(user.getWeight()).isEqualTo(60.0f);
    }

    @Test
    void 성별을_안_보내도_DB가_남성이면_임신으로_수정할_수_없다() {
        User user = user(Gender.M);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        assertThrows(InvalidPregnancy.class,
                () -> bodyEditService.editBody(3L, Role.GUARDIAN,
                        new BodyEditRequest(null, null, null, null, true)));

        assertThat(user.getIsPregnant()).isNull();
    }

    @Test
    void 남성이_임신_아님으로_수정하는_것은_허용한다() {
        User user = user(Gender.M);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        BodyEditResponse response = bodyEditService.editBody(3L, Role.GUARDIAN,
                new BodyEditRequest(null, null, null, null, false));

        assertThat(response.isPregnant()).isFalse();
    }
}
