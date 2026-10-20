package com.unify.onyak.domain.wards.link_codes.service.impl;

import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCode;
import com.unify.onyak.domain.wards.link_codes.reposiotry.LinkCodeRepository;
import com.unify.onyak.domain.wards.link_codes.response.LinkCodeResponse;
import com.unify.onyak.domain.wards.link_codes.service.LinkCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class LinkCodeServiceImpl implements LinkCodeService {

    private final LinkCodeRepository linkCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public LinkCodeResponse createLinkCode(Ward ward){

        int randomNum = secureRandom.nextInt(1_000_000);
        // 랜덤 숫자(0~100만)를 code로
        String code = String.format("%06d",randomNum);

        // 랜덤 UUID 생성
        String qrToken = UUID.randomUUID().toString();

        // 현재로 부터 5분 뒤의 시간을 만료 시간으로 설정
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(5);

        // 연동 코드 생성
        LinkCode linkCode = LinkCode.createLocal(
                ward,
                code,
                qrToken,
                expiresAt
        );

        linkCodeRepository.save(linkCode);

        return new LinkCodeResponse(linkCode.getCode(),linkCode.getQrToken(),linkCode.getExpiresAt());
    }
}
