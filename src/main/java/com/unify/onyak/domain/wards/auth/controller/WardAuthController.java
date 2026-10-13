package com.unify.onyak.domain.wards.auth.controller;

import com.unify.onyak.domain.wards.auth.dto.request.WardSignUpRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;
import com.unify.onyak.domain.wards.auth.service.WardSignUpService;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ward/auth")
public class WardAuthController {

    private final WardSignUpService wardSignUpService;

    @PostMapping("/signup")
    public WardSignUpResponse signup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid WardSignUpRequest request
    ){
        return wardSignUpService.signUp(userDetails,request);
    }
}
