package com.unify.onyak.domain.wards.auth.service.impl;

import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.error.exception.TooManyLoginRequestException;
import com.unify.onyak.domain.wards.auth.service.WardLoginAttemptService;
import com.unify.onyak.domain.wards.auth.service.WardLoginService;
import com.unify.onyak.domain.wards.link_codes.error.exception.InvalidLinkCodeException;
import com.unify.onyak.domain.wards.link_codes.service.VerifyLinkCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WardLoginServiceImpl implements WardLoginService {

    private final VerifyLinkCodeService verifyLinkCodeService;
    private final AuthService authService;
    private final WardLoginAttemptService wardLoginAttemptService;

    @Override
    @Transactional
    public LoginResponse login(WardLoginRequest request, String clientIp){

        if(wardLoginAttemptService.isBlocked(clientIp)){
            throw new TooManyLoginRequestException();
        }

        Ward ward;

        try{
           ward = verifyLinkCodeService.verifyLinkCode(request);
        }catch(InvalidLinkCodeException e){
            wardLoginAttemptService.recordFailure(clientIp);
            throw e;
        }

        TokenResponse token =  authService.issueTokens(ward.getId(),Role.WARD);

        wardLoginAttemptService.reset(clientIp);

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
