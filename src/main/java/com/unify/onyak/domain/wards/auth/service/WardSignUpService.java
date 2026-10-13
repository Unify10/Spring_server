package com.unify.onyak.domain.wards.auth.service;

import com.unify.onyak.domain.wards.auth.dto.request.WardSignUpRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;
import com.unify.onyak.global.security.CustomUserDetails;

public interface WardSignUpService {
    WardSignUpResponse signUp(CustomUserDetails userDetails, WardSignUpRequest request);
}
