package com.unify.onyak.domain.wards.link_codes.service.impl;

import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCode;
import com.unify.onyak.domain.wards.link_codes.error.exception.InvalidLinkCodeException;
import com.unify.onyak.domain.wards.link_codes.repository.LinkCodeRepository;
import com.unify.onyak.domain.wards.link_codes.service.VerifyLinkCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus.PENDING;

@RequiredArgsConstructor
@Service
public class VerifyLinkCodeServiceImpl implements VerifyLinkCodeService {

    private final LinkCodeRepository linkCodeRepository;

    @Override
    @Transactional
    public Ward verifyLinkCode(WardLoginRequest request) {

        boolean hasCode = request.code() != null && !request.code().isBlank();
        boolean hasQrToken = request.qrToken() != null && !request.qrToken().isBlank();
        // 코드인지 qr토큰인지 확인

        if(hasCode==hasQrToken){
            throw new InvalidLinkCodeException();
        } // 둘 중 하나만 입력되도록 예외처리

        LinkCode linkCode;

        if(hasCode){
            linkCode = linkCodeRepository.findByCodeAndStatusAndExpiresAtAfter(request.code(),PENDING,OffsetDateTime.now())
                    .orElseThrow(InvalidLinkCodeException::new);
        } else{
            linkCode = linkCodeRepository.findByQrTokenAndStatus(request.qrToken(), PENDING)
                    .orElseThrow(InvalidLinkCodeException::new);
        }

        OffsetDateTime now = OffsetDateTime.now();

        if(linkCode.isExpired(now)){
            linkCode.expire();
            throw new InvalidLinkCodeException();
        } // 만료 확인

        linkCode.markUsed(now); // 연동 코드 사용 처리

        return linkCode.getWardId();
    }
}
