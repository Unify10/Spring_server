package com.unify.onyak.domain.wards.link_codes.repository;

import com.unify.onyak.domain.wards.link_codes.entity.LinkCode;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface LinkCodeRepository extends JpaRepository<LinkCode,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LinkCode> findByCodeAndStatus(String code, LinkCodeStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LinkCode> findByQrTokenAndStatus(String qrToken, LinkCodeStatus status);
}
