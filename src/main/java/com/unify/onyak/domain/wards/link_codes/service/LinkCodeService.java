package com.unify.onyak.domain.wards.link_codes.service;

import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.link_codes.response.LinkCodeResponse;

public interface LinkCodeService {
    LinkCodeResponse createLinkCode(Ward ward);
}
