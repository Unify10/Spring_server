package com.unify.onyak.domain.drugs.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI GET /drugs/{drugId} 응답 중 drugs 테이블을 채우는 데 필요한 필드만. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DrugDetail(
        @JsonProperty("drug_id") String drugId,
        @JsonProperty("item_name") String itemName,
        @JsonProperty("entp_name") String entpName,
        @JsonProperty("main_ingredient") String mainIngredient,
        @JsonProperty("efficacy") String efficacy,
        @JsonProperty("usage") String usage,
        @JsonProperty("caution") String caution
) {
}
