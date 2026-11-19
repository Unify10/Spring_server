package com.unify.onyak.domain.medications.entity;

import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.medications.error.exception.InvalidEndDateException;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 가상 약장 항목 (onyak.sql 의 medications 테이블).
 * <ul>
 *   <li>5.1 약장: ward 가 먹는 drug + 용량 + 기간</li>
 *   <li>5.2 중단: active=false + end_date (기록은 남고 재개 가능)</li>
 *   <li>5.3 일정: schedule_times(매일 복용 시각) + push_enabled</li>
 * </ul>
 * updated_at 컬럼이 없어서 BaseTimeEntity 를 상속하지 않는다.
 */
@Entity
@Table(name = "medications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Ward ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(nullable = false, length = 100)
    private String dosage;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "schedule_times", nullable = false, columnDefinition = "time[]")
    private List<LocalTime> scheduleTimes = new ArrayList<>();

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static Medication create(Ward ward, Drug drug, String dosage, LocalDate startDate,
                                    List<LocalTime> scheduleTimes, boolean pushEnabled) {
        Medication medication = new Medication();
        medication.ward = ward;
        medication.drug = drug;
        medication.dosage = dosage;
        medication.startDate = startDate;
        medication.scheduleTimes = new ArrayList<>(scheduleTimes);
        medication.pushEnabled = pushEnabled;
        medication.active = true;
        return medication;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public void changeDosage(String dosage) {
        this.dosage = dosage;
    }

    /** 복용 중단 (이미 중단된 약이면 종료일만 고친다). */
    public void stop(LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidEndDateException();
        }
        this.active = false;
        this.endDate = endDate;
    }

    /** 복용 재개 — 종료일을 지운다. 같은 약 중복 여부는 서비스에서 먼저 확인한다. */
    public void resume() {
        this.active = true;
        this.endDate = null;
    }

    public void updateSchedule(List<LocalTime> scheduleTimes, boolean pushEnabled) {
        this.scheduleTimes = new ArrayList<>(scheduleTimes);
        this.pushEnabled = pushEnabled;
    }

    /** 실제로 복약 알림이 울려야 하는 상태인지 (약 단위 + 피보호자 전체 설정). */
    public boolean isReminderActive() {
        return isActive()
                && Boolean.TRUE.equals(pushEnabled)
                && Boolean.TRUE.equals(ward.getMedicationPushEnabled())
                && !scheduleTimes.isEmpty();
    }
}
