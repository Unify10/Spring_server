package com.unify.onyak.domain.wards.link_codes.service;

import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.entity.Ward;

public interface VerifyLinkCodeService {
    Ward verifyLinkCode(WardLoginRequest request);
}
