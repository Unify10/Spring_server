package com.unify.onyak.domain.medications.repository;

import com.unify.onyak.domain.medications.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    /** 약장 목록. drug 를 함께 가져와 N+1 을 막는다. 복용 중 → 최근 등록 순. */
    @Query("""
            select m from Medication m
            join fetch m.drug
            where m.ward.id = :wardId and m.active in :actives
            order by m.active desc, m.createdAt desc, m.id desc
            """)
    List<Medication> findAllByWardWithDrug(@Param("wardId") Long wardId,
                                           @Param("actives") Collection<Boolean> actives);

    /** 단건. 권한 검사와 알림 상태 계산에 ward 가, 응답에 drug 가 필요하다. */
    @Query("""
            select m from Medication m
            join fetch m.drug
            join fetch m.ward
            where m.id = :id
            """)
    Optional<Medication> findWithDrugAndWardById(@Param("id") Long id);

    boolean existsByWardIdAndDrugIdAndActiveTrue(Long wardId, Long drugId);

    boolean existsByWardIdAndDrugIdAndActiveTrueAndIdNot(Long wardId, Long drugId, Long id);
}
