package com.unify.onyak.domain.wards.auth.controller;

import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.dto.request.WardSignUpRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;
import com.unify.onyak.domain.wards.auth.service.WardLoginService;
import com.unify.onyak.domain.wards.auth.service.WardSignUpService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
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
    public ApiResponse<WardSignUpResponse> signup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid WardSignUpRequest request
    ){
        return ApiResponse.created(wardSignUpService.signUp(userDetails,request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
           @RequestBody WardLoginRequest request,
           HttpServletRequest httpServletRequest
    ){
        String clientIp = httpServletRequest.getRemoteAddr();
        return ApiResponse.ok(wardLoginService.login(request,clientIp));
    }
}
