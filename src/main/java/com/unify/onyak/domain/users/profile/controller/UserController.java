package com.unify.onyak.domain.users.profile.controller;

import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;
import com.unify.onyak.domain.users.profile.service.SettingEditService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final SettingEditService settingEditService;
    private final CurrentUserProvider currentUserProvider;

    //앱 환경설정 변경
    @PatchMapping("/me/settings")
    public ApiResponse<SettingEditResponse> editSetting(@RequestBody @Valid SettingEditRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        return ApiResponse.ok(settingEditService.editSetting(userId, request));
    }
}
