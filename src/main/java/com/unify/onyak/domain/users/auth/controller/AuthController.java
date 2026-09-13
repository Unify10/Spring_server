package com.unify.onyak.domain.users.auth.controller;

import com.unify.onyak.domain.users.auth.service.UserSignUpService;
import com.unify.onyak.domain.users.auth.dto.request.SignUpRequest;
import com.unify.onyak.domain.users.auth.dto.response.SignUpResponse;
import com.unify.onyak.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final UserSignUpService userSignUpService;

    @PostMapping("/signup")
    public ApiResponse<SignUpResponse> signUp(@RequestBody @Valid SignUpRequest request) {
        return ApiResponse.ok(userSignUpService.signUp(request));
    }
}
