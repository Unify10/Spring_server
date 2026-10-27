package com.unify.onyak.domain.wards.auth.service.impl;

import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.service.WardLoginService;
import com.unify.onyak.domain.wards.link_codes.service.VerifyLinkCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WardLoginServiceImpl implements WardLoginService {

    private final VerifyLinkCodeService verifyLinkCodeService;
    private final AuthService authService;

    @Override
    @Transactional
    public LoginResponse login(WardLoginRequest request){

        Ward ward = verifyLinkCodeService.verifyLinkCode(request);

        TokenResponse token =  authService.issueTokens(ward.getId(),Role.WARD);

        return new LoginResponse(
                ward.getId(),
                null,
                ward.getName(),
                Role.WARD,
                token.accessToken(),
                token.refreshToken()
        );
    }
}
