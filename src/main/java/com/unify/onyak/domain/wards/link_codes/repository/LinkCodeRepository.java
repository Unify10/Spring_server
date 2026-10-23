package com.unify.onyak.domain.wards.link_codes.repository;

import com.unify.onyak.domain.wards.link_codes.entity.LinkCode;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkCodeRepository extends JpaRepository<LinkCode,Long> {
    Optional<LinkCode> findByCodeAndStatus(String code, LinkCodeStatus status);
    Optional<LinkCode> findByQrTokenAndStatus(String qrToken, LinkCodeStatus status);
}
