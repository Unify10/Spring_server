package com.unify.onyak.domain.drugs.repository;

import com.unify.onyak.domain.drugs.entity.Drug;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DrugRepository extends JpaRepository<Drug, Long> {
    Optional<Drug> findByItemSeq(String itemSeq);
}
