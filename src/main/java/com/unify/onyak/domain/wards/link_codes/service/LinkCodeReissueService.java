package com.unify.onyak.domain.wards.link_codes.service;

import com.unify.onyak.domain.wards.link_codes.response.LinkCodeResponse;
import com.unify.onyak.global.security.CustomUserDetails;

public interface LinkCodeReissueService {
    LinkCodeResponse reissue(CustomUserDetails userDetails, Long wardId);
}
