package com.unify.onyak.domain.wards.auth.entity;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@Table(name = "wards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ward extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false)
    private User guardianId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name="birth_date",nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private Float weight;

    @Column(nullable = false)
    private Float height;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "is_pregnant")
    private Boolean isPregnant = false;

    @Column(name = "medication_push_enabled",nullable = false)
    private Boolean medicationPushEnabled = true;

    @Column(name = "sos_push_enabled",nullable = false)
    private Boolean sosPushEnabled = true;

    public static Ward createLocal(User guardianId, String name, LocalDate birthDate, Float weight, Float height,
                            Gender gender, Boolean isPregnant, Boolean medicationPushEnabled,
                            Boolean sosPushEnabled){
        return new Ward(
                null,
                guardianId,
                name,
                birthDate,
                weight,
                height,
                gender,
                isPregnant,
                medicationPushEnabled,
                sosPushEnabled
        );
    }
}
