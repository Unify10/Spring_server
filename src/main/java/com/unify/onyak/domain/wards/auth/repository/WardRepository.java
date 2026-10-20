package com.unify.onyak.domain.wards.auth.repository;

import com.unify.onyak.domain.wards.auth.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WardRepository extends JpaRepository<Ward,Long> {
}
