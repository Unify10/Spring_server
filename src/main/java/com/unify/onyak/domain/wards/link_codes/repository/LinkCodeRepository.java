package com.unify.onyak.domain.wards.link_codes.repository;

import com.unify.onyak.domain.wards.link_codes.entity.LinkCode;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface LinkCodeRepository extends JpaRepository<LinkCode,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LinkCode> findByCodeAndStatusAndExpiresAtAfter(String code, LinkCodeStatus status, OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LinkCode> findByQrTokenAndStatus(String qrToken, LinkCodeStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE LinkCode l
    SET l.status = :expiredStatus
    WHERE l.status = :pendingStatus
      AND l.expiresAt <= :now
""") int expirePendingCodes(
            @Param("now") OffsetDateTime now,
            @Param("pendingStatus") LinkCodeStatus pendingStatus,
            @Param("expiredStatus") LinkCodeStatus expiredStatus
    );

    @Modifying
    @Query(value = """
            INSERT INTO link_codes (ward_id, code, qr_token, status, expires_at, created_at)
            VALUES (:wardId, :code, :qrToken, 'PENDING', :expiresAt, now())
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfCodeAvailable(
            @Param("wardId") Long wardId,
            @Param("code") String code,
            @Param("qrToken") String qrToken,
            @Param("expiresAt") OffsetDateTime expiresAt
    );
}
