package com.unify.onyak.domain.notifications.controller;

import com.unify.onyak.domain.notifications.service.SosService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import com.unify.onyak.domain.users.auth.entity.Role;  
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.unify.onyak.domain.notifications.dto.request.SosRequest;

@RestController
@RequiredArgsConstructor
public class SosController {

    private final SosService sosService;

    @PostMapping("/sos")
    public ApiResponse<Void> sendSos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SosRequest request
    )      {
        sosService.sendSos(
                userDetails.getUserId(),
                Role.valueOf(userDetails.getRoleName()),
                request
        );
        return ApiResponse.ok(null);
    }
}
