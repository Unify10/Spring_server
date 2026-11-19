package com.unify.onyak.domain.wards.profile.controller;

import com.unify.onyak.domain.wards.profile.dto.request.WardProfileEditRequest;
import com.unify.onyak.domain.wards.profile.dto.response.WardDetailResponse;
import com.unify.onyak.domain.wards.profile.dto.response.WardListResponse;
import com.unify.onyak.domain.wards.profile.dto.response.WardProfileEditResponse;
import com.unify.onyak.domain.wards.profile.service.WardProfileEditService;
import com.unify.onyak.domain.wards.profile.service.WardProfileService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wards")
public class WardProfileController {

    private final WardProfileEditService wardProfileEditService;
    private final WardProfileService wardProfileService;

    @PatchMapping("/{wardId}")
    public ApiResponse<WardProfileEditResponse> editWard(
            @PathVariable long wardId,
            @AuthenticationPrincipal CustomUserDetails guardianUserDetails,
            @RequestBody @Valid WardProfileEditRequest request) {
        return ApiResponse.ok(wardProfileEditService.editWardProfile(
                wardId,
                guardianUserDetails,
                request));
    }

    @GetMapping
    public ApiResponse<List<WardListResponse>> getWards(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(wardProfileService.getWards(userDetails));
    }

    @GetMapping("/{wardId}")
    public ApiResponse<WardDetailResponse> getWard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wardId) {
        return ApiResponse.ok(wardProfileService.getWard(userDetails, wardId));
    }
}
