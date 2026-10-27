package com.unify.onyak.domain.users.profile.controller;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.profile.dto.request.MyProfileEditRequest;
import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.MyProfileEditResponse;
import com.unify.onyak.domain.users.profile.dto.response.MyProfileResponse;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;
import com.unify.onyak.domain.users.profile.service.MyProfileEditService;
import com.unify.onyak.domain.users.profile.service.MyProfileService;
import com.unify.onyak.domain.users.profile.service.SettingEditService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final SettingEditService settingEditService;
    private final MyProfileService myProfileService;
    private final MyProfileEditService myProfileEditService;

    // 내 정보 조회
    @GetMapping("/me")
    public ApiResponse<MyProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(myProfileService.getMyProfile(userDetails.getUserId(), Role.valueOf(userDetails.getRoleName())));
    }

    @PatchMapping("/me")
    public ApiResponse<MyProfileEditResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MyProfileEditRequest request
    ) {
        return ApiResponse.ok(myProfileEditService.editMyProfile(userDetails.getUserId(), Role.valueOf(userDetails.getRoleName()), request));
    }

    //앱 환경설정 변경
    @PatchMapping("/me/settings")
    public ApiResponse<SettingEditResponse> editSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid SettingEditRequest request
    ) {
        return ApiResponse.ok(settingEditService.editSetting(userDetails.getUserId(), request));
    }

}
