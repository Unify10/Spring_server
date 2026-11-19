package com.unify.onyak.domain.wards.auth.dto.request;

public record WardLoginRequest(
        String code,
        String qrToken
) {
}
