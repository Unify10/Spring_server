package com.unify.onyak.domain.wards.link_codes.entity;

import com.unify.onyak.domain.wards.auth.entity.Ward;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

import static com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus.PENDING;

@Entity
@AllArgsConstructor
@Table(name = "link_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Ward wardId;

    @Column(length = 6, nullable = false)
    @Size(min = 6 ,max = 6, message = "연동 코드는 반드시 6자리입니다.")
    @Pattern(
            regexp = "\\d{6}",
            message = "연동 코드는 반드시 숫자여야 합니다."
    )
    private String code;

    @Column(name = "qr_token", nullable = false)
    private String qrToken;

    @Column(name = "attempt_count",nullable = false)
    private int attemptCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private LinkCodeStatus status = PENDING;

    @Column(name = "expires_at",nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at",nullable = false)
    private OffsetDateTime createdAt;

    public static LinkCode createLocal(
            Ward wardId, String code, String qrToken, OffsetDateTime expiresAt){
        return new LinkCode(
            null,
                wardId,
                code,
                qrToken,
                0,
                PENDING,
                expiresAt,
                null,
                null
        );
    }
}
