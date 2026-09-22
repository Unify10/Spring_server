package com.unify.onyak.domain.app.dto.response;

import lombok.Builder;

@Builder
public record AppInfoResponse(
        String version,

        String noticeUrl,

        String termsUrl,

        String privacyUrl
)
{ }
