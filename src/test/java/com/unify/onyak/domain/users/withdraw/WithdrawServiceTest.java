package com.unify.onyak.domain.users.withdraw;

import com.unify.onyak.domain.notifications.entity.RecipientType;
import com.unify.onyak.domain.notifications.repository.NotificationRepository;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.withdraw.error.exception.WithdrawForbiddenException;
import com.unify.onyak.domain.users.withdraw.service.WithdrawService;
import com.unify.onyak.domain.users.withdraw.service.impl.WithdrawServiceImpl;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import com.unify.onyak.domain.users.auth.error.exception.UserIdNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WithdrawServiceTest {

    UserRepository userRepository;
    WardRepository wardRepository;
    NotificationRepository notificationRepository;
    RefreshTokenRepository refreshTokenRepository;
    WithdrawService withdrawService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        wardRepository = mock(WardRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        withdrawService = new WithdrawServiceImpl(
                userRepository, wardRepository, notificationRepository, refreshTokenRepository);
    }

    @Test
    void 피보호자_role이면_아무것도_삭제하지_않고_예외를_던진다() {
        assertThrows(WithdrawForbiddenException.class,
                () -> withdrawService.withdraw(3L, Role.WARD));

        verifyNoInteractions(userRepository, wardRepository, notificationRepository, refreshTokenRepository);
    }

    @Test
    void 존재하지_않는_사용자면_예외를_던진다() {
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

    // 테스트의 assertThrows 클래스 교체
        assertThrows(UserIdNotFoundException.class,
                () -> withdrawService.withdraw(3L, Role.GUARDIAN));

        verify(userRepository, never()).delete(any());
    }

    @Test
    void 보호자와_피보호자의_데이터와_토큰을_모두_삭제한다() {
        User user = mock(User.class);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        Ward ward = mock(Ward.class);
        when(ward.getId()).thenReturn(7L);
        List<Ward> wards = List.of(ward);
        when(wardRepository.findByGuardianId_Id(3L)).thenReturn(wards);

        withdrawService.withdraw(3L, Role.GUARDIAN);

        verify(notificationRepository).deleteByRecipientTypeAndRecipientId(RecipientType.WARD, 7L);
        verify(notificationRepository).deleteByRecipientTypeAndRecipientId(RecipientType.GUARDIAN, 3L);
        verify(wardRepository).deleteAll(wards);
        verify(userRepository).delete(user);
        verify(refreshTokenRepository).deleteById(RefreshToken.keyOf(Role.WARD, 7L));
        verify(refreshTokenRepository).deleteById(RefreshToken.keyOf(Role.GUARDIAN, 3L));
    }

    @Test
    void 삭제_순서는_알림_피보호자_보호자_토큰_순이다() {
        User user = mock(User.class);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        Ward ward = mock(Ward.class);
        when(ward.getId()).thenReturn(7L);
        List<Ward> wards = List.of(ward);
        when(wardRepository.findByGuardianId_Id(3L)).thenReturn(wards);

        withdrawService.withdraw(3L, Role.GUARDIAN);

        InOrder inOrder = inOrder(notificationRepository, wardRepository, userRepository, refreshTokenRepository);
        inOrder.verify(notificationRepository).deleteByRecipientTypeAndRecipientId(RecipientType.WARD, 7L);
        inOrder.verify(notificationRepository).deleteByRecipientTypeAndRecipientId(RecipientType.GUARDIAN, 3L);
        inOrder.verify(wardRepository).deleteAll(wards);
        inOrder.verify(userRepository).delete(user);
        inOrder.verify(refreshTokenRepository).deleteById(RefreshToken.keyOf(Role.WARD, 7L));
        inOrder.verify(refreshTokenRepository).deleteById(RefreshToken.keyOf(Role.GUARDIAN, 3L));
    }
}