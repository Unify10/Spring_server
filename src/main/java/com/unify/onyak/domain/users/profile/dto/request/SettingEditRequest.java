package com.unify.onyak.domain.users.profile.dto.request;

public record SettingEditRequest(
        Boolean medication_push_enabled,

        Boolean sos_push_enabled,

        Boolean marketing_push_enabled
) {
}
