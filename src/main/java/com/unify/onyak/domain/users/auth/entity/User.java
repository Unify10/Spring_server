package com.unify.onyak.domain.users.auth.entity;

import com.unify.onyak.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
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

    @Column(length = 20)
    private String phone;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    private Float weight;

    private Float height;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Gender gender;

    @Column(name = "is_pregnant")
    private Boolean isPregnant;

    @Column(name = "medication_push_enabled", nullable = false)
    private Boolean medicationPushEnabled = true;

    @Column(name = "sos_push_enabled", nullable = false)
    private Boolean sosPushEnabled = true;

    @Column(name = "marketing_push_enabled", nullable = false)
    private Boolean marketingPushEnabled = true;

    public static User createLocal(String email, String passwordHash, String name, String phone,
                                    LocalDate birthDate, Float weight, Float height, Gender gender,
                                    Boolean isPregnant,
                                    Boolean medicationPushEnabled, Boolean sosPushEnabled, Boolean marketingPushEnabled) {
        return new User(
                null,
                email,
                passwordHash,
                null,
                name,
                phone,
                false,
                birthDate,
                weight,
                height,
                gender,
                isPregnant,
                medicationPushEnabled,
                sosPushEnabled,
                marketingPushEnabled
        );
    }

    public void updateSettings(
            Boolean medicationPushEnabled,
            Boolean sosPushEnabled,
            Boolean marketingPushEnabled) {
        if(medicationPushEnabled != null) this.medicationPushEnabled = medicationPushEnabled;
        if(sosPushEnabled != null) this.sosPushEnabled = sosPushEnabled;
        if(marketingPushEnabled != null) this.marketingPushEnabled = marketingPushEnabled;
    }

    public static User createGoogle(String email, String name, String googleId) {
        return new User(
                null,
                email,
                null,
                googleId,
                name,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                true,
                true,
                true
        );
    }

    public void linkGoogleId(String googleId) {
        this.googleId = googleId;
    }
}
