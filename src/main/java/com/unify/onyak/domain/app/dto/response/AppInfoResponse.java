package com.unify.onyak.domain.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AppInfoResponse(
        String version,

        @JsonProperty("notice_url")
        String noticeUrl,

        @JsonProperty("terms_url")
        String termsUrl,

        @JsonProperty("privacy_url")
        String privacyUrl
)
{
}
