package com.unify.onyak.domain.wards.profile.controller;

import com.unify.onyak.domain.wards.profile.dto.response.WardDetailResponse;
import com.unify.onyak.domain.wards.profile.dto.response.WardListResponse;
import com.unify.onyak.domain.wards.profile.service.WardProfileService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wards")
public class WardProfileController {

    private final WardProfileService wardProfileService;

    @GetMapping
    public ApiResponse<List<WardListResponse>> getWards(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ApiResponse.ok(wardProfileService.getWards(userDetails));
    }

    @GetMapping("/{wardId}")
    public ApiResponse<WardDetailResponse> getWard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wardId
    ){
        return ApiResponse.ok(wardProfileService.getWard(userDetails,wardId));
    }


}
