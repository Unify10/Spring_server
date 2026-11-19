package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.medications.dto.request.MedicationCheckRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationCheckResponse;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.entity.MedicationCheck;
import com.unify.onyak.domain.medications.error.exception.*;
import com.unify.onyak.domain.medications.policy.KoreanDate;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.repository.MedicationCheckRepository;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.unify.onyak.domain.medications.support.MedicationFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MedicationCheckServiceImplTest {

    static final long GUARDIAN_ID = 3L;
    static final long WARD_ID = 7L;
    static final long MED_ID = 10L;
    static final LocalTime EIGHT = LocalTime.of(8, 0);

    MedicationRepository medicationRepository;
    MedicationCheckRepository medicationCheckRepository;
    WardRepository wardRepository;
    MedicationCheckServiceImpl service;

    Ward ward;
    Drug tylenol;

    @BeforeEach
    void setUp() {
        medicationRepository = mock(MedicationRepository.class);
        medicationCheckRepository = mock(MedicationCheckRepository.class);
        wardRepository = mock(WardRepository.class);
        service = new MedicationCheckServiceImpl(
                medicationRepository, medicationCheckRepository, new WardAccessValidator(wardRepository));

        ward = ward(WARD_ID, GUARDIAN_ID);
        tylenol = drug(1L, "195700020", "타이레놀정500mg", List.of("아세트아미노펜"));
        when(medicationCheckRepository.save(any(MedicationCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Medication stored(List<LocalTime> times) {
        Medication medication = medication(MED_ID, ward, tylenol, times);
        when(medicationRepository.findWithDrugAndWardById(MED_ID)).thenReturn(Optional.of(medication));
        return medication;
    }

    private MedicationCheckRequest at(String time) {
        return new MedicationCheckRequest(time);
    }

    @Test
    void 보호자가_복용_일정에_있는_시각을_체크한다() {
        stored(List.of(EIGHT, LocalTime.of(20, 0)));

        MedicationCheckResponse response = service.check(guardian(GUARDIAN_ID), MED_ID, at("08:00"));

        assertThat(response.medicationId()).isEqualTo(MED_ID);
        assertThat(response.scheduleTime()).isEqualTo("08:00");
        verify(medicationCheckRepository).save(any(MedicationCheck.class));
    }

    @Test
    void 피보호자_본인도_체크할_수_있다() {
        stored(List.of(EIGHT));

        MedicationCheckResponse response = service.check(wardUser(WARD_ID), MED_ID, at("08:00"));

        assertThat(response.scheduleTime()).isEqualTo("08:00");
        verify(medicationCheckRepository).save(any(MedicationCheck.class));
    }

    @Test
    void 다른_보호자의_피보호자_약은_체크할_수_없다() {
        stored(List.of(EIGHT));

        assertThatThrownBy(() -> service.check(guardian(99L), MED_ID, at("08:00")))
                .isInstanceOf(WardAccessDeniedException.class);

        verify(medicationCheckRepository, never()).save(any());
    }

    @Test
    void 다른_피보호자는_체크할_수_없다() {
        stored(List.of(EIGHT));

        assertThatThrownBy(() -> service.check(wardUser(99L), MED_ID, at("08:00")))
                .isInstanceOf(WardAccessDeniedException.class);

        verify(medicationCheckRepository, never()).save(any());
    }

    @Test
    void 존재하지_않는_약이면_404_예외다() {
        when(medicationRepository.findWithDrugAndWardById(MED_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.check(guardian(GUARDIAN_ID), MED_ID, at("08:00")))
                .isInstanceOf(MedicationNotFoundException.class);
    }

    @Test
    void 복용_중단된_약은_체크할_수_없다() {
        Medication medication = stored(List.of(EIGHT));
        medication.stop(KoreanDate.today());

        assertThatThrownBy(() -> service.check(guardian(GUARDIAN_ID), MED_ID, at("08:00")))
                .isInstanceOf(InactiveMedicationException.class);

        verify(medicationCheckRepository, never()).save(any());
    }

    @Test
    void 복용_일정에_없는_시각은_체크할_수_없다() {
        stored(List.of(EIGHT));

        assertThatThrownBy(() -> service.check(guardian(GUARDIAN_ID), MED_ID, at("13:00")))
                .isInstanceOf(CheckTimeNotScheduledException.class);

        verify(medicationCheckRepository, never()).save(any());
    }

    @Test
    void 오늘_이미_체크한_시각이면_저장하지_않고_예외를_던진다() {
        stored(List.of(EIGHT));
        when(medicationCheckRepository.existsChecked(eq(MED_ID), eq(EIGHT), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.check(guardian(GUARDIAN_ID), MED_ID, at("08:00")))
                .isInstanceOf(CheckAlreadyDoneException.class);

        verify(medicationCheckRepository, never()).save(any());
    }

    @Test
    void 중복_확인은_한국_시간_오늘_하루_범위로_한다() {
        stored(List.of(EIGHT));

        service.check(guardian(GUARDIAN_ID), MED_ID, at("08:00"));

        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(medicationCheckRepository).existsChecked(eq(MED_ID), eq(EIGHT), from.capture(), to.capture());

        assertThat(from.getValue().toLocalDate()).isEqualTo(KoreanDate.today());
        assertThat(from.getValue().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(to.getValue()).isEqualTo(from.getValue().plusDays(1));
    }
}