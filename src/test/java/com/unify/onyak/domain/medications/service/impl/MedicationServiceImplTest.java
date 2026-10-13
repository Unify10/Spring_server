package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.drugs.service.DrugService;
import com.unify.onyak.domain.medications.dto.request.MedicationCreateRequest;
import com.unify.onyak.domain.medications.dto.request.MedicationUpdateRequest;
import com.unify.onyak.domain.medications.dto.request.ScheduleRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationResponse;
import com.unify.onyak.domain.medications.dto.response.ScheduleResponse;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.error.exception.*;
import com.unify.onyak.domain.medications.policy.KoreanDate;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.unify.onyak.domain.medications.support.MedicationFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class MedicationServiceImplTest {

    static final long GUARDIAN_ID = 3L;
    static final long WARD_ID = 7L;

    MedicationRepository medicationRepository;
    WardRepository wardRepository;
    DrugService drugService;
    MedicationServiceImpl service;

    Ward ward;
    Drug tylenol;
    CustomUserDetails me;

    @BeforeEach
    void setUp() {
        medicationRepository = mock(MedicationRepository.class);
        wardRepository = mock(WardRepository.class);
        drugService = mock(DrugService.class);
        service = new MedicationServiceImpl(medicationRepository, drugService, new WardAccessValidator(wardRepository));

        ward = ward(WARD_ID, GUARDIAN_ID);
        tylenol = drug(1L, "195700020", "타이레놀정500mg", List.of("아세트아미노펜"));
        me = guardian(GUARDIAN_ID);
        when(wardRepository.findById(WARD_ID)).thenReturn(Optional.of(ward));
    }

    private Medication stored(long id, List<LocalTime> times) {
        Medication m = medication(id, ward, tylenol, times);
        when(medicationRepository.findWithDrugAndWardById(id)).thenReturn(Optional.of(m));
        return m;
    }

    // ── 5.1 ─────────────────────────────────────────────────────
    @Test
    void 약을_담으면_시작일은_오늘_알림은_켜짐_시각은_정렬된다() {
        when(drugService.getOrFetch("195700020")).thenReturn(tylenol);
        when(medicationRepository.save(any(Medication.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicationResponse res = service.addMedication(me, WARD_ID,
                new MedicationCreateRequest(" 195700020 ", "1정", null, List.of("20:00", "08:00"), null));

        assertThat(res.drugId()).isEqualTo("195700020");
        assertThat(res.drugName()).isEqualTo("타이레놀정500mg");
        assertThat(res.startDate()).isEqualTo(KoreanDate.today());
        assertThat(res.pushEnabled()).isTrue();
        assertThat(res.active()).isTrue();
        assertThat(res.scheduleTimes()).containsExactly("08:00", "20:00");
    }

    @Test
    void 같은_약이_복용중이면_409() {
        when(drugService.getOrFetch("195700020")).thenReturn(tylenol);
        when(medicationRepository.existsByWardIdAndDrugIdAndActiveTrue(WARD_ID, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.addMedication(me, WARD_ID,
                new MedicationCreateRequest("195700020", "1정", null, null, null)))
                .isInstanceOf(DuplicateMedicationException.class);
        verify(medicationRepository, never()).save(any());
    }

    @Test
    void 남의_피보호자_약장에는_담을_수_없다() {
        assertThatThrownBy(() -> service.addMedication(guardian(99L), WARD_ID,
                new MedicationCreateRequest("195700020", "1정", null, null, null)))
                .isInstanceOf(WardAccessDeniedException.class);
        verifyNoInteractions(drugService);
    }

    @Test
    void 목록은_status에_맞는_active_값으로_조회한다() {
        when(medicationRepository.findAllByWardWithDrug(anyLong(), any())).thenReturn(List.of());

        service.getMedications(me, WARD_ID, "active");
        service.getMedications(me, WARD_ID, "stopped");
        service.getMedications(me, WARD_ID, "all");

        verify(medicationRepository).findAllByWardWithDrug(WARD_ID, List.of(true));
        verify(medicationRepository).findAllByWardWithDrug(WARD_ID, List.of(false));
        verify(medicationRepository).findAllByWardWithDrug(WARD_ID, List.of(true, false));
        assertThatThrownBy(() -> service.getMedications(me, WARD_ID, "deleted"))
                .isInstanceOf(InvalidStatusFilterException.class);
    }

    @Test
    void 없는_약장_항목은_404() {
        when(medicationRepository.findWithDrugAndWardById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMedication(me, 55L))
                .isInstanceOf(MedicationNotFoundException.class);
    }

    @Test
    void 약장_항목도_ward_기준으로_권한을_본다() {
        stored(10L, List.of());

        assertThatThrownBy(() -> service.getMedication(guardian(99L), 10L))
                .isInstanceOf(WardAccessDeniedException.class);
        assertThat(service.getMedication(wardUser(WARD_ID), 10L).medicationId()).isEqualTo(10L);
    }

    // ── 5.2 ─────────────────────────────────────────────────────
    @Test
    void 중단하면_종료일이_오늘로_설정된다() {
        stored(10L, List.of());

        MedicationResponse res = service.updateMedication(me, 10L, new MedicationUpdateRequest(null, false, null));

        assertThat(res.active()).isFalse();
        assertThat(res.endDate()).isEqualTo(KoreanDate.today());
    }

    @Test
    void 중단일이_시작일보다_빠르면_400() {
        stored(10L, List.of());   // 시작일 2026-10-01

        assertThatThrownBy(() -> service.updateMedication(me, 10L,
                new MedicationUpdateRequest(null, false, LocalDate.of(2026, 9, 1))))
                .isInstanceOf(InvalidEndDateException.class);
    }

    @Test
    void 재개하면_종료일이_지워진다() {
        Medication m = stored(10L, List.of());
        m.stop(LocalDate.of(2026, 10, 5));

        MedicationResponse res = service.updateMedication(me, 10L, new MedicationUpdateRequest(null, true, null));

        assertThat(res.active()).isTrue();
        assertThat(res.endDate()).isNull();
    }

    @Test
    void 같은_약이_이미_복용중이면_재개는_409() {
        Medication m = stored(10L, List.of());
        m.stop(LocalDate.of(2026, 10, 5));
        when(medicationRepository.existsByWardIdAndDrugIdAndActiveTrueAndIdNot(WARD_ID, 1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateMedication(me, 10L, new MedicationUpdateRequest(null, true, null)))
                .isInstanceOf(DuplicateMedicationException.class);
        assertThat(m.isActive()).isFalse();
    }

    @Test
    void 보낸_필드만_바꾼다() {
        stored(10L, List.of());

        MedicationResponse res = service.updateMedication(me, 10L, new MedicationUpdateRequest("2정", null, null));

        assertThat(res.dosage()).isEqualTo("2정");
        assertThat(res.active()).isTrue();
        assertThat(res.endDate()).isNull();
    }

    @Test
    void 복용중인_약에_종료일만_보내면_400() {
        stored(10L, List.of());

        assertThatThrownBy(() -> service.updateMedication(me, 10L,
                new MedicationUpdateRequest(null, null, LocalDate.of(2026, 10, 20))))
                .isInstanceOf(EndDateOnActiveMedicationException.class);
    }

    @Test
    void 삭제는_실제로_지운다() {
        Medication m = stored(10L, List.of());

        service.deleteMedication(me, 10L);

        verify(medicationRepository).delete(m);
    }

    // ── 5.3 ─────────────────────────────────────────────────────
    @Test
    void 일정을_정렬해서_저장하고_알림_상태를_계산한다() {
        Medication m = stored(10L, List.of());

        ScheduleResponse res = service.updateSchedule(me, 10L, new ScheduleRequest(List.of("21:30", "07:00"), true));

        assertThat(res.scheduleTimes()).containsExactly("07:00", "21:30");
        assertThat(m.getScheduleTimes()).containsExactly(LocalTime.of(7, 0), LocalTime.of(21, 30));
        assertThat(res.reminderActive()).isTrue();
    }

    @Test
    void 알림을_끄거나_시각이_없으면_reminder_active는_false() {
        stored(10L, List.of());

        assertThat(service.updateSchedule(me, 10L, new ScheduleRequest(List.of("08:00"), false)).reminderActive())
                .isFalse();
        assertThat(service.updateSchedule(me, 10L, new ScheduleRequest(List.of(), true)).reminderActive())
                .isFalse();
    }

    @Test
    void 피보호자_전체_복약알림이_꺼져_있으면_reminder_active는_false() {
        Ward muted = ward(WARD_ID, GUARDIAN_ID, null, false, false);
        Medication m = medication(11L, muted, tylenol, List.of(LocalTime.of(8, 0)));
        when(medicationRepository.findWithDrugAndWardById(11L)).thenReturn(Optional.of(m));

        assertThat(service.getSchedule(me, 11L).reminderActive()).isFalse();
    }

    @Test
    void 중단된_약에는_일정을_설정할_수_없다() {
        Medication m = stored(10L, List.of());
        m.stop(LocalDate.of(2026, 10, 5));

        assertThatThrownBy(() -> service.updateSchedule(me, 10L, new ScheduleRequest(List.of("08:00"), true)))
                .isInstanceOf(InactiveMedicationException.class);
    }

    @Test
    void 시각_검증은_서비스에서도_한_번_더_한다() {
        assertThatThrownBy(() -> MedicationServiceImpl.parseTimes(List.of("08:00", "08:00")))
                .isInstanceOf(InvalidScheduleTimeException.class);
        assertThatThrownBy(() -> MedicationServiceImpl.parseTimes(List.of("8:00")))
                .isInstanceOf(InvalidScheduleTimeException.class);
        assertThatThrownBy(() -> MedicationServiceImpl.parseTimes(List.of("24:00")))
                .isInstanceOf(InvalidScheduleTimeException.class);
        assertThat(MedicationServiceImpl.parseTimes(null)).isEmpty();
    }
}
