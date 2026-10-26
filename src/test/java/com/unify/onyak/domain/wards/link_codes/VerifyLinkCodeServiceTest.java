package com.unify.onyak.domain.wards.link_codes;

import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCode;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import com.unify.onyak.domain.wards.link_codes.error.exception.InvalidLinkCodeException;
import com.unify.onyak.domain.wards.link_codes.repository.LinkCodeRepository;
import com.unify.onyak.domain.wards.link_codes.service.VerifyLinkCodeService;
import com.unify.onyak.domain.wards.link_codes.service.impl.VerifyLinkCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class VerifyLinkCodeServiceTest {

    private LinkCodeRepository linkCodeRepository;
    private VerifyLinkCodeService verifyLinkCodeService;

    @BeforeEach
    void setUp() {
        linkCodeRepository = mock(LinkCodeRepository.class);
        verifyLinkCodeService = new VerifyLinkCodeServiceImpl(linkCodeRepository);
    }

    @Test
    void 정상적인_연동코드는_피보호자를_반환하고_USED로_변경한다() {
        Ward ward = mock(Ward.class);
        LinkCode linkCode = linkCode(ward, OffsetDateTime.now().plusMinutes(5));
        when(linkCodeRepository.findByCodeAndStatus("123456", PENDING))
                .thenReturn(Optional.of(linkCode));

        Ward result = verifyLinkCodeService.verifyLinkCode(
                new WardLoginRequest("123456", null)
        );

        assertEquals(ward, result);
        assertEquals(LinkCodeStatus.USED, linkCode.getStatus());
        verify(linkCodeRepository).findByCodeAndStatus("123456", PENDING);
    }

    @Test
    void 정상적인_QR토큰은_피보호자를_반환하고_USED로_변경한다() {
        Ward ward = mock(Ward.class);
        LinkCode linkCode = linkCode(ward, OffsetDateTime.now().plusMinutes(5));
        when(linkCodeRepository.findByQrTokenAndStatus("qr-token", PENDING))
                .thenReturn(Optional.of(linkCode));

        Ward result = verifyLinkCodeService.verifyLinkCode(
                new WardLoginRequest(null, "qr-token")
        );

        assertEquals(ward, result);
        assertEquals(LinkCodeStatus.USED, linkCode.getStatus());
        verify(linkCodeRepository).findByQrTokenAndStatus("qr-token", PENDING);
    }

    @Test
    void 만료된_연동코드는_EXPIRED로_변경하고_예외를_던진다() {
        Ward ward = mock(Ward.class);
        LinkCode linkCode = linkCode(ward, OffsetDateTime.now().minusMinutes(1));
        when(linkCodeRepository.findByCodeAndStatus("123456", PENDING))
                .thenReturn(Optional.of(linkCode));

        assertThrows(
                InvalidLinkCodeException.class,
                () -> verifyLinkCodeService.verifyLinkCode(
                        new WardLoginRequest("123456", null)
                )
        );

        assertEquals(LinkCodeStatus.EXPIRED, linkCode.getStatus());
    }

    @Test
    void code와_QR토큰이_모두_없으면_예외를_던진다() {
        assertThrows(
                InvalidLinkCodeException.class,
                () -> verifyLinkCodeService.verifyLinkCode(
                        new WardLoginRequest(null, null)
                )
        );

        verifyNoInteractions(linkCodeRepository);
    }

    @Test
    void code와_QR토큰이_모두_있으면_예외를_던진다() {
        assertThrows(
                InvalidLinkCodeException.class,
                () -> verifyLinkCodeService.verifyLinkCode(
                        new WardLoginRequest("123456", "qr-token")
                )
        );

        verifyNoInteractions(linkCodeRepository);
    }

    @Test
    void 존재하지_않는_연동코드는_예외를_던진다() {
        when(linkCodeRepository.findByCodeAndStatus("999999", PENDING))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidLinkCodeException.class,
                () -> verifyLinkCodeService.verifyLinkCode(
                        new WardLoginRequest("999999", null)
                )
        );
    }

    private LinkCode linkCode(Ward ward, OffsetDateTime expiresAt) {
        return LinkCode.createLocal(ward, "123456", "qr-token", expiresAt);
    }
}
