package com.unify.onyak.domain.medications.repository;

import com.unify.onyak.domain.medications.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
}
