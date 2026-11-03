package com.unify.onyak.domain.wards.link_codes;

import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import com.unify.onyak.domain.wards.link_codes.error.exception.LinkCodeGenerationFailedException;
import com.unify.onyak.domain.wards.link_codes.repository.LinkCodeRepository;
import com.unify.onyak.domain.wards.link_codes.response.LinkCodeResponse;
import com.unify.onyak.domain.wards.link_codes.service.LinkCodeService;
import com.unify.onyak.domain.wards.link_codes.service.impl.LinkCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkCodeServiceTest {

    private LinkCodeRepository linkCodeRepository;
    private LinkCodeService linkCodeService;

    @BeforeEach
    void setUp() {
        linkCodeRepository = mock(LinkCodeRepository.class);
        linkCodeService = new LinkCodeServiceImpl(linkCodeRepository);

        when(linkCodeRepository.expirePendingCodes(
                any(OffsetDateTime.class),
                eq(LinkCodeStatus.PENDING),
                eq(LinkCodeStatus.EXPIRED)
        )).thenReturn(0);
    }

    @Test
    void 만료된_PENDING_코드를_정리한_후_중복되지_않은_코드를_생성한다() {
        Ward ward = mock(Ward.class);
        when(linkCodeRepository.insertIfCodeAvailable(
                any(Long.class),
                any(String.class),
                any(String.class),
                any(OffsetDateTime.class)
        )).thenReturn(1);

        LinkCodeResponse response = linkCodeService.createLinkCode(ward);

        assertEquals(6, response.code().length());
        verify(linkCodeRepository).expirePendingCodes(
                any(OffsetDateTime.class),
                eq(LinkCodeStatus.PENDING),
                eq(LinkCodeStatus.EXPIRED)
        );
        verify(linkCodeRepository).insertIfCodeAvailable(
                any(Long.class),
                any(String.class),
                any(String.class),
                any(OffsetDateTime.class)
        );
    }

    @Test
    void 생성된_코드가_중복되면_새로운_코드를_재생성한다() {
        Ward ward = mock(Ward.class);
        when(linkCodeRepository.insertIfCodeAvailable(
                any(Long.class),
                any(String.class),
                any(String.class),
                any(OffsetDateTime.class)
        )).thenReturn(0, 1);

        LinkCodeResponse response = linkCodeService.createLinkCode(ward);

        assertEquals(6, response.code().length());
        verify(linkCodeRepository, org.mockito.Mockito.times(2))
                .insertIfCodeAvailable(
                        any(Long.class),
                        any(String.class),
                        any(String.class),
                        any(OffsetDateTime.class)
                );
    }

    @Test
    void 코드가_10회_연속_중복되면_생성_실패_예외를_던진다() {
        Ward ward = mock(Ward.class);
        when(linkCodeRepository.insertIfCodeAvailable(
                any(Long.class),
                any(String.class),
                any(String.class),
                any(OffsetDateTime.class)
        )).thenReturn(0);

        assertThrows(
                LinkCodeGenerationFailedException.class,
                () -> linkCodeService.createLinkCode(ward)
        );

        verify(linkCodeRepository, org.mockito.Mockito.times(10))
                .insertIfCodeAvailable(
                        any(Long.class),
                        any(String.class),
                        any(String.class),
                        any(OffsetDateTime.class)
                );
    }
}
