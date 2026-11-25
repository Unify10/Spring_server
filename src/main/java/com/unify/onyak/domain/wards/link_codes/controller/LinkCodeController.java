package com.unify.onyak.domain.wards.link_codes.controller;

import com.unify.onyak.domain.wards.link_codes.response.LinkCodeResponse;
import com.unify.onyak.domain.wards.link_codes.service.LinkCodeReissueService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LinkCodeController {

    private final LinkCodeReissueService linkCodeReissueService;

    // 연동 코드 재발급 (보호자 전용)
    @PostMapping("/wards/{wardId}/link-code")
    public ApiResponse<LinkCodeResponse> reissueLinkCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wardId
    ) {
        return ApiResponse.created(linkCodeReissueService.reissue(userDetails, wardId));
    }
}
