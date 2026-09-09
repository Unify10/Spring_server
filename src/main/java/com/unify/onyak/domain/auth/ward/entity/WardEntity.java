package com.unify.onyak.domain.auth.ward.entity;

import com.unify.onyak.domain.auth.users.entity.Gender;
import com.unify.onyak.domain.auth.users.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Date;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Long Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false)
    private UserEntity guardianId;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
