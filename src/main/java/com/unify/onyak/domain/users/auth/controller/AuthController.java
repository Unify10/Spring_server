package com.unify.onyak.domain.users.auth.controller;

import com.unify.onyak.domain.users.auth.dto.request.LoginRequest;
import com.unify.onyak.domain.users.auth.dto.request.RefreshRequest;
import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.users.auth.service.EmailVerificationService;
import com.unify.onyak.domain.users.auth.service.UserSignUpService;
import com.unify.onyak.domain.users.auth.dto.request.EmailSendRequest;
import com.unify.onyak.domain.users.auth.dto.request.EmailVerifyRequest;
import com.unify.onyak.domain.users.auth.dto.request.SignUpRequest;
import com.unify.onyak.domain.users.auth.dto.response.SignUpResponse;
import com.unify.onyak.global.response.ApiResponse;
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
@RequestMapping("/auth")
public class AuthController {
    private final UserSignUpService userSignUpService;

    private final AuthService authService;

    private final EmailVerificationService emailVerificationService;

    // 회원가입
    @PostMapping("/signup")
    public ApiResponse<SignUpResponse> signUp(@RequestBody @Valid SignUpRequest request) {
        return ApiResponse.ok(userSignUpService.signUp(request));
    }

    // 인증 이메일 보냄
    @PostMapping("/email/send")
    public ApiResponse<Void> sendEmailCode(@RequestBody @Valid EmailSendRequest request) {
        emailVerificationService.sendCode(request.email());
        return ApiResponse.ok(null);
    }

    // 인증 코드 검증
    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmailCode(@RequestBody @Valid EmailVerifyRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ApiResponse.ok(null);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    //로그아웃 기능
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUserId(), Role.valueOf(userDetails.getRoleName()));
        return ApiResponse.ok(null);
    }
}
