package com.unify.onyak.domain.users.withdraw.controller;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.withdraw.service.WithdrawService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @DeleteMapping("/users/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        withdrawService.withdraw(
                userDetails.getUserId(),
                Role.valueOf(userDetails.getRoleName())
        );
        return ApiResponse.ok(null);
    }
}