package com.unify.onyak.domain.users.profile.dto.request;

public record SettingEditRequest(
        Boolean medicationPushEnabled,

        Boolean sosPushEnabled,

        Boolean marketingPushEnabled
) {
}
