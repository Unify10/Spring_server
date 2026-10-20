package com.unify.onyak.domain.medications.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

/**
 * 근거 카드(6.2)를 만들 때마다 약별로 남기는 분석 결과 스냅샷 (analysis_results 테이블).
 * <p>
 * detail 은 JSON 문자열을 그대로 jsonb 로 넣는다. Hibernate 의 JSON 타입 매핑은 Jackson 2 를
 * 찾는데 이 프로젝트 웹 계층은 Jackson 3 이라, 형식 변환기 의존 없이 캐스트로 처리한다.
 */
@Entity
@Table(name = "analysis_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Medication medication;

    @Column(name = "has_warning", nullable = false)
    private Boolean hasWarning = false;

    @Column(nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String detail;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private OffsetDateTime generatedAt;

    public static AnalysisResult of(Medication medication, boolean hasWarning, String detailJson) {
        AnalysisResult result = new AnalysisResult();
        result.medication = medication;
        result.hasWarning = hasWarning;
        result.detail = detailJson;
        return result;
    }
}
