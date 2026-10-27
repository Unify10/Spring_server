package com.unify.onyak.domain.wards.link_codes.repository;

import com.unify.onyak.domain.wards.link_codes.entity.LinkCode;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface LinkCodeRepository extends JpaRepository<LinkCode,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LinkCode> findByCodeAndStatusAndExpiresAtAfter(String code, LinkCodeStatus status, OffsetDateTime now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LinkCode> findByQrTokenAndStatus(String qrToken, LinkCodeStatus status);
}
