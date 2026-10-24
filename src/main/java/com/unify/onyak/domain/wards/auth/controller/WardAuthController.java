package com.unify.onyak.domain.wards.auth.controller;

import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.dto.request.WardSignUpRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;
import com.unify.onyak.domain.wards.auth.service.WardLoginService;
import com.unify.onyak.domain.wards.auth.service.WardSignUpService;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ward/auth")
public class WardAuthController {

    private final WardSignUpService wardSignUpService;
    private final WardLoginService wardLoginService;

    @PostMapping("/signup")
    public WardSignUpResponse signup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid WardSignUpRequest request
    ){
        return wardSignUpService.signUp(userDetails,request);
    }

    @PostMapping("/login")
    public LoginResponse login(
           @RequestBody WardLoginRequest request
    ){
        return wardLoginService.login(request);
    }
}
