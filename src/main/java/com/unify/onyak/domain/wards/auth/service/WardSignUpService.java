package com.unify.onyak.domain.wards.auth.service;

import com.unify.onyak.domain.wards.auth.dto.request.WardSignUpRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;

public interface WardSignUpService {
    WardSignUpResponse signUp(WardSignUpRequest request);
}
