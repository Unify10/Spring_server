package com.unify.onyak.domain.users.profile.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SettingEditRequest(
        @JsonProperty("medicationPushEnabled")
        Boolean medicationPushEnabled,

        @JsonProperty("sosPushEnabled")
        Boolean sosPushEnabled,

        @JsonProperty("marketingPushEnabled")
        Boolean marketingPushEnabled
) {
}
