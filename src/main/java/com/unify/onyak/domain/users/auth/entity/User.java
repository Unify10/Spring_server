package com.unify.onyak.domain.users.auth.entity;

import com.unify.onyak.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    private Integer age;

    private Float weight;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Gender gender;

    @Column(name = "is_pregnant")
    private Boolean isPregnant;

    @Builder.Default
    @Column(name = "medication_push_enabled", nullable = false)
    private Boolean medicationPushEnabled = true;

    @Builder.Default
    @Column(name = "sos_push_enabled", nullable = false)
    private Boolean sosPushEnabled = true;
}
