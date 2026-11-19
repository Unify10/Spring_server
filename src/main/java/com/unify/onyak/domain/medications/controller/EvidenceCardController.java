package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse;
import com.unify.onyak.domain.medications.service.EvidenceCardService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 6.2 복약 확인 근거 카드. */
@RestController
@RequiredArgsConstructor
public class EvidenceCardController {

    private final EvidenceCardService evidenceCardService;

    @GetMapping("/wards/{wardId}/evidence-card")
    public ApiResponse<EvidenceCardResponse> getEvidenceCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wardId
    ) {
        return ApiResponse.ok(evidenceCardService.getEvidenceCard(userDetails, wardId));
    }
}
