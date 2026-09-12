package com.unify.onyak.domain.wards.auth.entity;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "wards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ward extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false)
    private User guardianId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name="birth_date",nullable = false)
    private Date birthDate;

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false)
    private double height;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "is_pregnant")
    private Boolean isPregnant = false;

    @Column(name = "medication_push_enabled",nullable = false)
    private Boolean medicationPushEnabled = true;

    @Column(name = "sos_push_enabled",nullable = false)
    private Boolean sosPushEnabled = true;
}
