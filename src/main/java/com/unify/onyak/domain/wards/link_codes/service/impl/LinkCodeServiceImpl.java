package com.unify.onyak.domain.wards.link_codes.service.impl;

import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import com.unify.onyak.domain.wards.link_codes.error.exception.LinkCodeGenerationFailedException;
import com.unify.onyak.domain.wards.link_codes.repository.LinkCodeRepository;
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

        linkCodeRepository.expirePendingCodes(OffsetDateTime.now(), LinkCodeStatus.PENDING,LinkCodeStatus.EXPIRED);

        for(int i = 0; i<10; i++){

            int randomNum = secureRandom.nextInt(1_000_000);
            // 랜덤 숫자(0~100만)를 code로
            String code = String.format("%06d",randomNum);

            OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(5);
            String qrToken = UUID.randomUUID().toString();

            int inserted = linkCodeRepository.insertIfCodeAvailable(
                    ward.getId(),
                    code,
                    qrToken,
                    expiresAt
            );

            if(inserted == 1){
                return new LinkCodeResponse(code, qrToken, expiresAt);
            }
        }

        throw new LinkCodeGenerationFailedException();
    }
}
