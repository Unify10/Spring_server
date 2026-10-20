package com.unify.onyak.domain.wards.auth.repository;

import com.unify.onyak.domain.wards.auth.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WardRepository extends JpaRepository<Ward,Long> {
    List<Ward> findByGuardianId_Id(Long guardianId);
}