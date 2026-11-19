package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.request.MedicationCheckRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationCheckResponse;
import com.unify.onyak.domain.medications.service.MedicationCheckService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MedicationCheckController {

    private final MedicationCheckService medicationCheckService;

    // 5.4 복약 완료 체크 — 보호자·피보호자 모두
    @PostMapping("/medications/{medId}/check")
    public ApiResponse<MedicationCheckResponse> check(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long medId,
            @RequestBody @Valid MedicationCheckRequest request
    ) {
        return ApiResponse.created(medicationCheckService.check(userDetails, medId, request));
    }
}