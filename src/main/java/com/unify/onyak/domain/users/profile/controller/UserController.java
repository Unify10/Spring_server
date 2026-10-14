package com.unify.onyak.domain.users.profile.controller;

import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;
import com.unify.onyak.domain.users.profile.service.SettingEditService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final SettingEditService settingEditService;

    //앱 환경설정 변경
    @PatchMapping("/me/settings")
    public ApiResponse<SettingEditResponse> editSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid SettingEditRequest request
    ) {
        return ApiResponse.ok(settingEditService.editSetting(userDetails.getUserId(), request));
    }
}
