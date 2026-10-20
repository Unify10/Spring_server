package com.unify.onyak.domain.medications.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "medication_checks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Medication medication;

    @Column(name = "schedule_time", nullable = false)
    private LocalTime scheduleTime;

    @CreationTimestamp
    @Column(name = "checked_at", nullable = false, updatable = false)
    private OffsetDateTime checkedAt;

    public static MedicationCheck create(Medication medication, LocalTime scheduleTime) {
        MedicationCheck check = new MedicationCheck();
        check.medication = medication;
        check.scheduleTime = scheduleTime;
        return check;
    }
}