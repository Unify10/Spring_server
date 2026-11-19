package com.unify.onyak.domain.medications.service;

import com.unify.onyak.domain.medications.dto.request.MedicationCheckRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationCheckResponse;
import com.unify.onyak.global.security.CustomUserDetails;

public interface MedicationCheckService {
    MedicationCheckResponse check(CustomUserDetails user, Long medicationId, MedicationCheckRequest request);
}