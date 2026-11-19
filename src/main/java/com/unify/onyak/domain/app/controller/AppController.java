package com.unify.onyak.domain.app.controller;

import com.unify.onyak.domain.app.dto.response.AppInfoResponse;
import com.unify.onyak.domain.app.service.AppService;
import com.unify.onyak.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app")
public class AppController {
    private final AppService appService;

    @GetMapping("/info")
    public ApiResponse<AppInfoResponse> getAppInfo(){
        return ApiResponse.ok(appService.getAppInfo());
    }
}
