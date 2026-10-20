package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.medications.dto.request.MedicationCheckRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationCheckResponse;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.entity.MedicationCheck;
import com.unify.onyak.domain.medications.error.exception.CheckAlreadyDoneException;
import com.unify.onyak.domain.medications.error.exception.CheckTimeNotScheduledException;
import com.unify.onyak.domain.medications.error.exception.InactiveMedicationException;
import com.unify.onyak.domain.medications.error.exception.MedicationNotFoundException;
import com.unify.onyak.domain.medications.policy.KoreanDate;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.medications.repository.MedicationCheckRepository;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.medications.service.MedicationCheckService;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class MedicationCheckServiceImpl implements MedicationCheckService {

    private final MedicationRepository medicationRepository;
    private final MedicationCheckRepository medicationCheckRepository;
    private final WardAccessValidator wardAccessValidator;

    @Override
    @Transactional
    public MedicationCheckResponse check(CustomUserDetails user, Long medicationId, MedicationCheckRequest request) {
        Medication medication = medicationRepository.findWithDrugAndWardById(medicationId)
                .orElseThrow(MedicationNotFoundException::new);
        wardAccessValidator.check(user, medication.getWard(), Access.WARD_OR_GUARDIAN);

        if (!medication.isActive()) {
            throw new InactiveMedicationException();
        }

        LocalTime scheduleTime = LocalTime.parse(request.scheduleTime());
        if (!medication.getScheduleTimes().contains(scheduleTime)) {
            throw new CheckTimeNotScheduledException();
        }

        LocalDate today = KoreanDate.today();
        OffsetDateTime from = today.atStartOfDay(KoreanDate.ZONE).toOffsetDateTime();
        OffsetDateTime to = today.plusDays(1).atStartOfDay(KoreanDate.ZONE).toOffsetDateTime();
        if (medicationCheckRepository.existsChecked(medicationId, scheduleTime, from, to)) {
            throw new CheckAlreadyDoneException();
        }

        MedicationCheck saved = medicationCheckRepository.save(MedicationCheck.create(medication, scheduleTime));
        return MedicationCheckResponse.from(saved);
    }
}