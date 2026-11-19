package com.unify.onyak.domain.drugs.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** FastAPI POST /drugs/cabinet-check 응답. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CabinetCheckResponse(
        @JsonProperty("level") String level,
        @JsonProperty("headline") String headline,
        @JsonProperty("medications") List<Medication> medications,
        @JsonProperty("interactions") List<Interaction> interactions,
        @JsonProperty("warnings") List<String> warnings,
        @JsonProperty("disclaimer") String disclaimer
) {
    public CabinetCheckResponse {
        medications = medications == null ? List.of() : medications;
        interactions = interactions == null ? List.of() : interactions;
        warnings = warnings == null ? List.of() : warnings;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Medication(
            @JsonProperty("key") String key,
            @JsonProperty("drug_id") String drugId,
            @JsonProperty("item_name") String itemName,
            @JsonProperty("classification") String classification,
            @JsonProperty("confidence") Double confidence,
            @JsonProperty("evidences") List<Evidence> evidences,
            @JsonProperty("findings") List<Finding> findings,
            @JsonProperty("summary") Map<String, Integer> summary,
            @JsonProperty("lookup_failed") boolean lookupFailed
    ) {
        public Medication {
            evidences = evidences == null ? List.of() : evidences;
            findings = findings == null ? List.of() : findings;
            summary = summary == null ? Map.of() : summary;
        }
    }

    /** 약장 안 두 약 사이의 문제 (병용금기 / 성분중복). keys 는 요청 때 넣은 key. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Interaction(
            @JsonProperty("kind") String kind,
            @JsonProperty("severity") String severity,
            @JsonProperty("keys") List<String> keys,
            @JsonProperty("item_names") List<String> itemNames,
            @JsonProperty("detail") String detail
    ) {
        public Interaction {
            keys = keys == null ? List.of() : keys;
            itemNames = itemNames == null ? List.of() : itemNames;
        }
    }

    /** 전문/일반 판정 근거 하나 (허가정보·DUR 품목정보 등). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Evidence(
            @JsonProperty("source") String source,
            @JsonProperty("verdict") String verdict,
            @JsonProperty("detail") String detail,
            @JsonProperty("weight") Integer weight
    ) {
    }

    /** DUR 점검 결과 한 건. severity: CONTRAINDICATED(금기) / CAUTION(주의). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Finding(
            @JsonProperty("check_key") String checkKey,
            @JsonProperty("check_label") String checkLabel,
            @JsonProperty("severity") String severity,
            @JsonProperty("prohibit_content") String prohibitContent,
            @JsonProperty("counterpart") String counterpart,
            @JsonProperty("remark") String remark
    ) {
    }
}
