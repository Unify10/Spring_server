package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.request.MedicationCreateRequest;
import com.unify.onyak.domain.medications.dto.request.MedicationUpdateRequest;
import com.unify.onyak.domain.medications.dto.request.ScheduleRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationResponse;
import com.unify.onyak.domain.medications.dto.response.ScheduleResponse;
import com.unify.onyak.domain.medications.service.MedicationService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 5.1 가상 약장 / 5.2 삭제·중단 / 5.3 복용 일정·알림.
 * 권한: 보호자는 자기 피보호자, 피보호자는 본인 약장만.
 */
@RestController
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;

    // 5.1 약장 목록
    @GetMapping("/wards/{wardId}/medications")
    public ApiResponse<List<MedicationResponse>> getMedications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wardId,
            @RequestParam(defaultValue = "active") String status
    ) {
        return ApiResponse.ok(medicationService.getMedications(userDetails, wardId, status));
    }

    // 5.1 약장에 담기
    @PostMapping("/wards/{wardId}/medications")
    public ApiResponse<MedicationResponse> addMedication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wardId,
            @RequestBody @Valid MedicationCreateRequest request
    ) {
        return ApiResponse.created(medicationService.addMedication(userDetails, wardId, request));
    }

    // 5.1 단건 조회
    @GetMapping("/medications/{medId}")
    public ApiResponse<MedicationResponse> getMedication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long medId
    ) {
        return ApiResponse.ok(medicationService.getMedication(userDetails, medId));
    }

    // 5.2 용량 수정 / 복용 중단 / 재개
    @PatchMapping("/medications/{medId}")
    public ApiResponse<MedicationResponse> updateMedication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long medId,
            @RequestBody @Valid MedicationUpdateRequest request
    ) {
        return ApiResponse.ok(medicationService.updateMedication(userDetails, medId, request));
    }

    // 5.2 삭제 (잘못 담은 약)
    @DeleteMapping("/medications/{medId}")
    public ResponseEntity<Void> deleteMedication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long medId
    ) {
        medicationService.deleteMedication(userDetails, medId);
        return ResponseEntity.noContent().build();
    }

    // 5.3 복용 일정 조회
    @GetMapping("/medications/{medId}/schedule")
    public ApiResponse<ScheduleResponse> getSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long medId
    ) {
        return ApiResponse.ok(medicationService.getSchedule(userDetails, medId));
    }

    // 5.3 복용 일정·알림 설정
    @PutMapping("/medications/{medId}/schedule")
    public ApiResponse<ScheduleResponse> updateSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long medId,
            @RequestBody @Valid ScheduleRequest request
    ) {
        return ApiResponse.ok(medicationService.updateSchedule(userDetails, medId, request));
    }
}
