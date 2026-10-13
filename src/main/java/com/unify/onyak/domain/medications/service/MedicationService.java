package com.unify.onyak.domain.medications.service;

import com.unify.onyak.domain.medications.dto.request.MedicationCreateRequest;
import com.unify.onyak.domain.medications.dto.request.MedicationUpdateRequest;
import com.unify.onyak.domain.medications.dto.request.ScheduleRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationResponse;
import com.unify.onyak.domain.medications.dto.response.ScheduleResponse;
import com.unify.onyak.global.security.CustomUserDetails;

import java.util.List;

public interface MedicationService {

    /** 5.1 약장 목록. status: active | stopped | all */
    List<MedicationResponse> getMedications(CustomUserDetails user, Long wardId, String status);

    /** 5.1 약장에 담기 */
    MedicationResponse addMedication(CustomUserDetails user, Long wardId, MedicationCreateRequest request);

    /** 5.1 단건 조회 */
    MedicationResponse getMedication(CustomUserDetails user, Long medicationId);

    /** 5.2 용량 수정 / 복용 중단 / 재개 */
    MedicationResponse updateMedication(CustomUserDetails user, Long medicationId, MedicationUpdateRequest request);

    /** 5.2 삭제 */
    void deleteMedication(CustomUserDetails user, Long medicationId);

    /** 5.3 복용 일정 조회 */
    ScheduleResponse getSchedule(CustomUserDetails user, Long medicationId);

    /** 5.3 복용 일정·알림 설정 */
    ScheduleResponse updateSchedule(CustomUserDetails user, Long medicationId, ScheduleRequest request);
}
