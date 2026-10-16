package com.unify.onyak.domain.medications.repository;

import com.unify.onyak.domain.medications.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    /** 약마다 가장 최근 스냅샷 1건 (근거 카드 중복 저장 방지용). */
    @Query("""
            select a from AnalysisResult a
            where a.medication.id in :medicationIds
              and a.id = (select max(b.id) from AnalysisResult b where b.medication.id = a.medication.id)
            """)
    List<AnalysisResult> findLatestByMedicationIds(@Param("medicationIds") Collection<Long> medicationIds);
}
