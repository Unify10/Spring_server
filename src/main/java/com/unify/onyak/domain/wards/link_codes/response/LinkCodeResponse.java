package com.unify.onyak.domain.wards.link_codes.response;

import java.time.OffsetDateTime;

public record LinkCodeResponse(
        String code,
        String qrToken,
        OffsetDateTime expiresAt
) {
}
