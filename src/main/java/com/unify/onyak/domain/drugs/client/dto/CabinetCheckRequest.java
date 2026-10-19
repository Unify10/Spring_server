package com.unify.onyak.domain.drugs.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** FastAPI POST /drugs/cabinet-check 요청. */
public record CabinetCheckRequest(
        @JsonProperty("items") List<Item> items,
        @JsonProperty("age") Integer age,
        @JsonProperty("pregnant") Boolean pregnant
) {
    /**
     * @param key 결과를 다시 매칭할 때 쓰는 값. medication id 를 문자열로 넣는다.
     */
    public record Item(
            @JsonProperty("key") String key,
            @JsonProperty("drug_id") String drugId,
            @JsonProperty("item_name") String itemName,
            @JsonProperty("main_ingredient") String mainIngredient
    ) {
    }
}
