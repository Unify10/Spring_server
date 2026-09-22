package com.unify.onyak.domain.users.profile.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record SettingEditResponse(
    Boolean medicationPushEnabled,

    Boolean sosPushEnabled,

    Boolean marketingPushEnabled
) {

}