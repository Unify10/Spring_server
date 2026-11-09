package com.unify.onyak.domain.wards.profile.controller;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.wards.profile.dto.request.WardProfileEditRequest;
import com.unify.onyak.domain.wards.profile.dto.response.WardProfileEditResponse;
import com.unify.onyak.domain.wards.profile.service.WardProfileEditService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wards")
public class WardProfileController {

    private final WardProfileEditService wardProfileEditService;

    @PatchMapping("/{wardId}")
    public ApiResponse<WardProfileEditResponse> editWard(
            @PathVariable long wardId,
            @AuthenticationPrincipal CustomUserDetails guardianUserDetails,
            @RequestBody @Valid WardProfileEditRequest request){
        return ApiResponse.ok(wardProfileEditService.editWardProfile(
                wardId,
                guardianUserDetails.getUserId(),
                Role.valueOf(guardianUserDetails.getRoleName()),
                request));
    }
}
