package com.unify.onyak.domain.wards.auth.service.impl;

import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.service.WardLoginService;
import com.unify.onyak.domain.wards.link_codes.service.VerifyLinkCodeService;
import com.unify.onyak.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WardLoginServiceImpl implements WardLoginService {

    private final VerifyLinkCodeService verifyLinkCodeService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public LoginResponse login(WardLoginRequest request){

        Ward ward = verifyLinkCodeService.verifyLinkCode(request);

        return issueToken(ward.getId(), Role.WARD);
    }

    private LoginResponse issueToken(Long wardId, Role role){
        String accessToken = jwtProvider.generateAccessToken(wardId, role);
        String refreshToken = jwtProvider.generateRefreshToken(wardId, role);

        refreshTokenRepository.save(new RefreshToken(
                wardId,
                role,
                refreshToken,
                jwtProvider.getRefreshTokenExpTime()
        ));

        return new LoginResponse(accessToken, refreshToken);
    }
}
