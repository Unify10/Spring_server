package com.unify.onyak.domain.wards.link_codes;

import com.unify.onyak.domain.medications.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.medications.error.exception.WardNotFoundException;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.error.exception.GuardianRoleRequiredException;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import com.unify.onyak.domain.wards.link_codes.repository.LinkCodeRepository;
import com.unify.onyak.domain.wards.link_codes.response.LinkCodeResponse;
import com.unify.onyak.domain.wards.link_codes.service.LinkCodeService;
import com.unify.onyak.domain.wards.link_codes.service.impl.LinkCodeReissueServiceImpl;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkCodeReissueServiceTest {

    private static final Long GUARDIAN_ID = 1L;
    private static final Long WARD_ID = 10L;

    @Mock private WardAccessValidator wardAccessValidator;
    @Mock private LinkCodeRepository linkCodeRepository;
    @Mock private LinkCodeService linkCodeService;

    @InjectMocks
    private LinkCodeReissueServiceImpl linkCodeReissueService;

    private CustomUserDetails guardian() {
        return new CustomUserDetails(GUARDIAN_ID, "guardian@test.com", null, "ROLE_GUARDIAN");
    }

    @Test
    @DisplayName("자기 피보호자면 이전 PENDING 코드를 만료시킨 뒤 새 코드를 발급한다")
    void 자기_피보호자면_새_코드를_발급한다() {
        CustomUserDetails user = guardian();
        Ward ward = mock(Ward.class);
        when(ward.getId()).thenReturn(WARD_ID);
        when(wardAccessValidator.validate(user, WARD_ID, Access.GUARDIAN_ONLY)).thenReturn(ward);
        LinkCodeResponse issued = new LinkCodeResponse("123456", "qr-token", OffsetDateTime.now().plusMinutes(5));
        when(linkCodeService.createLinkCode(ward)).thenReturn(issued);

        LinkCodeResponse response = linkCodeReissueService.reissue(user, WARD_ID);

        assertThat(response).isEqualTo(issued);
        InOrder order = inOrder(linkCodeRepository, linkCodeService);
        order.verify(linkCodeRepository).expirePendingCodesOfWard(WARD_ID, LinkCodeStatus.PENDING, LinkCodeStatus.EXPIRED);
        order.verify(linkCodeService).createLinkCode(ward);
    }

    @Test
    @DisplayName("피보호자 토큰으로 요청하면 보호자 권한 예외를 던지고 아무것도 바꾸지 않는다")
    void 피보호자가_요청하면_거부한다() {
        CustomUserDetails wardUser = new CustomUserDetails(WARD_ID, "ward", null, "ROLE_WARD");

        assertThatThrownBy(() -> linkCodeReissueService.reissue(wardUser, WARD_ID))
                .isInstanceOf(GuardianRoleRequiredException.class);

        verifyNoInteractions(wardAccessValidator, linkCodeRepository, linkCodeService);
    }

    @Test
    @DisplayName("다른 보호자의 피보호자면 접근 거부 예외를 던지고 코드를 만들지 않는다")
    void 남의_피보호자면_거부한다() {
        CustomUserDetails user = guardian();
        when(wardAccessValidator.validate(user, WARD_ID, Access.GUARDIAN_ONLY))
                .thenThrow(new WardAccessDeniedException());

        assertThatThrownBy(() -> linkCodeReissueService.reissue(user, WARD_ID))
                .isInstanceOf(WardAccessDeniedException.class);

        verify(linkCodeRepository, never()).expirePendingCodesOfWard(anyLong(), any(), any());
        verify(linkCodeService, never()).createLinkCode(any());
    }

    @Test
    @DisplayName("없는 피보호자면 찾을 수 없음 예외를 던진다")
    void 없는_피보호자면_예외를_던진다() {
        CustomUserDetails user = guardian();
        when(wardAccessValidator.validate(user, WARD_ID, Access.GUARDIAN_ONLY))
                .thenThrow(new WardNotFoundException());

        assertThatThrownBy(() -> linkCodeReissueService.reissue(user, WARD_ID))
                .isInstanceOf(WardNotFoundException.class);

        verify(linkCodeService, never()).createLinkCode(any());
    }
}
