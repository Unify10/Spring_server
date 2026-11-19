package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.medications.dto.response.CalendarDayResponse;
import com.unify.onyak.domain.medications.dto.response.CalendarResponse;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.policy.KoreanDate;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.medications.repository.MedicationCheckRepository;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.medications.service.MedicationCalendarService;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MedicationCalendarServiceImpl implements MedicationCalendarService {

    private final MedicationRepository medicationRepository;
    private final MedicationCheckRepository medicationCheckRepository;
    private final WardAccessValidator wardAccessValidator;

    @Override
    @Transactional(readOnly = true)
    public CalendarResponse getCalendar(CustomUserDetails user, Long wardId, YearMonth month) {
        wardAccessValidator.validate(user, wardId, Access.WARD_OR_GUARDIAN);

        LocalDate today = KoreanDate.today();
        YearMonth target = month != null ? month : YearMonth.from(today);

        List<Medication> medications = medicationRepository.findAllByWardWithDrug(wardId, List.of(true, false));

        OffsetDateTime from = target.atDay(1).atStartOfDay(KoreanDate.ZONE).toOffsetDateTime();
        OffsetDateTime to = target.plusMonths(1).atDay(1).atStartOfDay(KoreanDate.ZONE).toOffsetDateTime();
        Map<LocalDate, Integer> completedByDate = countByDate(
                medicationCheckRepository.findCheckedAtByWardBetween(wardId, from, to));

        List<CalendarDayResponse> days = new ArrayList<>();
        for (int day = 1; day <= target.lengthOfMonth(); day++) {
            LocalDate date = target.atDay(day);
            int scheduled = scheduledCount(medications, date);
            int completed = Math.min(completedByDate.getOrDefault(date, 0), scheduled);
            Double rate = (scheduled == 0 || date.isAfter(today))
                    ? null
                    : Math.round(completed * 1000.0 / scheduled) / 10.0;
            days.add(new CalendarDayResponse(date, scheduled, completed, rate));
        }

        return new CalendarResponse(wardId, target.toString(), days);
    }

    private int scheduledCount(List<Medication> medications, LocalDate date) {
        int count = 0;
        for (Medication medication : medications) {
            boolean started = !date.isBefore(medication.getStartDate());
            boolean notEnded = medication.getEndDate() == null || !date.isAfter(medication.getEndDate());
            if (started && notEnded) {
                count += medication.getScheduleTimes().size();
            }
        }
        return count;
    }

    private Map<LocalDate, Integer> countByDate(List<OffsetDateTime> checkedAts) {
        Map<LocalDate, Integer> counts = new HashMap<>();
        for (OffsetDateTime checkedAt : checkedAts) {
            LocalDate date = checkedAt.atZoneSameInstant(KoreanDate.ZONE).toLocalDate();
            counts.merge(date, 1, Integer::sum);
        }
        return counts;
    }
}