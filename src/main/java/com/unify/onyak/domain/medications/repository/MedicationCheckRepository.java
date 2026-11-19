package com.unify.onyak.domain.medications.repository;

import com.unify.onyak.domain.medications.entity.MedicationCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface MedicationCheckRepository extends JpaRepository<MedicationCheck, Long> {

    @Query("""
            select count(c) > 0 from MedicationCheck c
            where c.medication.id = :medicationId
              and c.scheduleTime = :scheduleTime
              and c.checkedAt >= :from
              and c.checkedAt < :to
            """)
    boolean existsChecked(@Param("medicationId") Long medicationId,
                          @Param("scheduleTime") LocalTime scheduleTime,
                          @Param("from") OffsetDateTime from,
                          @Param("to") OffsetDateTime to);

    @Query("""
            select c.checkedAt from MedicationCheck c
            where c.medication.ward.id = :wardId
              and c.checkedAt >= :from
              and c.checkedAt < :to
            """)
    List<OffsetDateTime> findCheckedAtByWardBetween(@Param("wardId") Long wardId,
                                                    @Param("from") OffsetDateTime from,
                                                    @Param("to") OffsetDateTime to);
}