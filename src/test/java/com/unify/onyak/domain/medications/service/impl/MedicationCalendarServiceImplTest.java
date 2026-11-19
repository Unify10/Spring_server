package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.medications.dto.response.CalendarDayResponse;
import com.unify.onyak.domain.medications.dto.response.CalendarResponse;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.repository.MedicationCheckRepository;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.unify.onyak.domain.medications.support.MedicationFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MedicationCalendarServiceImplTest {

    static final long GUARDIAN_ID = 3L;
    static final long WARD_ID = 7L;
    static final YearMonth PAST_MONTH = YearMonth.of(2026, 1);

    MedicationRepository medicationRepository;
    MedicationCheckRepository medicationCheckRepository;
    WardRepository wardRepository;
    MedicationCalendarServiceImpl service;

    Ward ward;
    Drug tylenol;

    @BeforeEach
    void setUp() {
        medicationRepository = mock(MedicationRepository.class);
        medicationCheckRepository = mock(MedicationCheckRepository.class);
        wardRepository = mock(WardRepository.class);
        service = new MedicationCalendarServiceImpl(
                medicationRepository, medicationCheckRepository, new WardAccessValidator(wardRepository));

        ward = ward(WARD_ID, GUARDIAN_ID);
        tylenol = drug(1L, "195700020", "타이레놀정500mg", List.of("아세트아미노펜"));
        when(wardRepository.findById(WARD_ID)).thenReturn(Optional.of(ward));
    }

    private Medication medicationAt(long id, LocalDate start, List<LocalTime> times) {
        Medication medication = medication(id, ward, tylenol, times);
        ReflectionTestUtils.setField(medication, "startDate", start);
        return medication;
    }

    private CalendarDayResponse day(CalendarResponse response, int dayOfMonth) {
        return response.days().get(dayOfMonth - 1);
    }

    private OffsetDateTime koreaTime(int month, int day, int hour) {
        return OffsetDateTime.of(2026, month, day, hour, 0, 0, 0, ZoneOffset.ofHours(9));
    }

    @Test
    void 한_달_전체_날짜가_나온다() {
        when(medicationRepository.findAllByWardWithDrug(eq(WARD_ID), any())).thenReturn(List.of());
        when(medicationCheckRepository.findCheckedAtByWardBetween(eq(WARD_ID), any(), any())).thenReturn(List.of());

        CalendarResponse response = service.getCalendar(guardian(GUARDIAN_ID), WARD_ID, PAST_MONTH);

        assertThat(response.wardId()).isEqualTo(WARD_ID);
        assertThat(response.month()).isEqualTo("2026-01");
        assertThat(response.days()).hasSize(31);
        assertThat(day(response, 1).date()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void 복용_일정이_없는_날은_예정이_0이고_달성률이_null이다() {
        when(medicationRepository.findAllByWardWithDrug(eq(WARD_ID), any())).thenReturn(List.of());
        when(medicationCheckRepository.findCheckedAtByWardBetween(eq(WARD_ID), any(), any())).thenReturn(List.of());

        CalendarResponse response = service.getCalendar(guardian(GUARDIAN_ID), WARD_ID, PAST_MONTH);

        assertThat(day(response, 5).scheduled()).isZero();
        assertThat(day(response, 5).rate()).isNull();
    }

    @Test
    void 예정과_완료로_달성률을_소수_첫째_자리까지_계산한다() {
        Medication m = medicationAt(10L, LocalDate.of(2026, 1, 1), List.of(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0)));
        when(medicationRepository.findAllByWardWithDrug(eq(WARD_ID), any())).thenReturn(List.of(m));
        when(medicationCheckRepository.findCheckedAtByWardBetween(eq(WARD_ID), any(), any()))
                .thenReturn(List.of(koreaTime(1, 10, 8), koreaTime(1, 10, 13)));

        CalendarResponse response = service.getCalendar(guardian(GUARDIAN_ID), WARD_ID, PAST_MONTH);

        CalendarDayResponse d = day(response, 10);
        assertThat(d.scheduled()).isEqualTo(3);
        assertThat(d.completed()).isEqualTo(2);
        assertThat(d.rate()).isEqualTo(66.7);
    }

    @Test
    void 완료가_예정보다_많아도_예정을_넘기지_않는다() {
        Medication m = medicationAt(10L, LocalDate.of(2026, 1, 1), List.of(LocalTime.of(8, 0)));
        when(medicationRepository.findAllByWardWithDrug(eq(WARD_ID), any())).thenReturn(List.of(m));
        when(medicationCheckRepository.findCheckedAtByWardBetween(eq(WARD_ID), any(), any()))
                .thenReturn(List.of(koreaTime(1, 12, 8), koreaTime(1, 12, 9), koreaTime(1, 12, 10)));

        CalendarResponse response = service.getCalendar(guardian(GUARDIAN_ID), WARD_ID, PAST_MONTH);

        assertThat(day(response, 12).completed()).isEqualTo(1);
        assertThat(day(response, 12).rate()).isEqualTo(100.0);
    }

    @Test
    void 날짜는_한국_시간_기준으로_센다() {
        Medication m = medicationAt(10L, LocalDate.of(2026, 1, 1), List.of(LocalTime.of(8, 0)));
        when(medicationRepository.findAllByWardWithDrug(eq(WARD_ID), any())).thenReturn(List.of(m));
        // UTC 1월 14일 16:00 = 한국 1월 15일 01:00
        when(medicationCheckRepository.findCheckedAtByWardBetween(eq(WARD_ID), any(), any()))
                .thenReturn(List.of(OffsetDateTime.of(2026, 1, 14, 16, 0, 0, 0, ZoneOffset.UTC)));

        CalendarResponse response = service.getCalendar(guardian(GUARDIAN_ID), WARD_ID, PAST_MONTH);

        assertThat(day(response, 14).completed()).isZero();
        assertThat(day(response, 15).completed()).isEqualTo(1);
    }

    @Test
    void 시작일_전과_종료일_후에는_예정에_포함하지_않는다() {
        Medication m = medicationAt(10L, LocalDate.of(2026, 1, 1), List.of(LocalTime.of(8, 0)));
        m.stop(LocalDate.of(2026, 1, 20));
        when(medicationRepository.findAllByWardWithDrug(eq(WARD_ID), any())).thenReturn(List.of(m));
        when(medicationCheckRepository.findCheckedAtByWardBetween(eq(WARD_ID), any(), any())).thenReturn(List.of());

        CalendarResponse response = service.getCalendar(guardian(GUARDIAN_ID), WARD_ID, PAST_MONTH);

        assertThat(day(response, 1).scheduled()).isEqualTo(1);
        assertThat(day(response, 20).scheduled()).isEqualTo(1);
        assertThat(day(response, 21).scheduled()).isZero();
    }

    @Test
    void 미래_날짜는_달성률이_null이다() {
        Medication m = medicationAt(10L, LocalDate.of(2026, 1, 1), List.of(LocalTime.of(8, 0)));
        when(medicationRepository.findAllByWardWithDrug(eq(WARD_ID), any())).thenReturn(List.of(m));
        when(medicationCheckRepository.findCheckedAtByWardBetween(eq(WARD_ID), any(), any())).thenReturn(List.of());

        CalendarResponse response = service.getCalendar(guardian(GUARDIAN_ID), WARD_ID, YearMonth.of(2099, 1));

        assertThat(day(response, 1).scheduled()).isEqualTo(1);
        assertThat(day(response, 1).rate()).isNull();
    }

    @Test
    void 다른_보호자의_피보호자_달력은_조회할_수_없다() {
        assertThatThrownBy(() -> service.getCalendar(guardian(99L), WARD_ID, PAST_MONTH))
                .isInstanceOf(WardAccessDeniedException.class);

        verifyNoInteractions(medicationRepository, medicationCheckRepository);
    }

    @Test
    void 피보호자_본인은_조회할_수_있다() {
        when(medicationRepository.findAllByWardWithDrug(eq(WARD_ID), any())).thenReturn(List.of());
        when(medicationCheckRepository.findCheckedAtByWardBetween(eq(WARD_ID), any(), any())).thenReturn(List.of());

        CalendarResponse response = service.getCalendar(wardUser(WARD_ID), WARD_ID, PAST_MONTH);

        assertThat(response.days()).hasSize(31);
    }
}