package com.unify.onyak.domain.drugs.entity;

import com.unify.onyak.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * 의약품 마스터 (onyak.sql 의 drugs 테이블).
 * <p>
 * 약장에 처음 담기는 순간 FastAPI(Python_server)에서 받아 채운다. → DrugService#getOrFetch
 */
@Entity
@Table(name = "drugs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drug extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 식약처 품목기준코드 — 앱/FastAPI 와 주고받는 약 식별자
    @Column(name = "item_seq", nullable = false, unique = true, length = 20)
    private String itemSeq;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> ingredients = new ArrayList<>();

    private String manufacturer;

    @Column(columnDefinition = "text")
    private String efficacy;

    @Column(name = "usage_info", columnDefinition = "text")
    private String usageInfo;

    @Column(columnDefinition = "text")
    private String precautions;

    @Column(unique = true, length = 50)
    private String barcode;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "efficacy_codes", nullable = false, columnDefinition = "text[]")
    private List<String> efficacyCodes = new ArrayList<>();

    @Column(name = "age_restriction", columnDefinition = "text")
    private String ageRestriction;

    @Column(name = "pregnancy_restriction", columnDefinition = "text")
    private String pregnancyRestriction;

    public static Drug create(String itemSeq, String name, List<String> ingredients, String manufacturer,
                              String efficacy, String usageInfo, String precautions) {
        Drug drug = new Drug();
        drug.itemSeq = itemSeq;
        drug.name = name;
        drug.ingredients = ingredients == null ? new ArrayList<>() : new ArrayList<>(ingredients);
        drug.manufacturer = manufacturer;
        drug.efficacy = efficacy;
        drug.usageInfo = usageInfo;
        drug.precautions = precautions;
        return drug;
    }
}
