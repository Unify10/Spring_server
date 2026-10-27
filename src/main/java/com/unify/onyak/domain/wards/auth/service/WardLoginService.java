package com.unify.onyak.domain.wards.auth.service;

import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;

public interface WardLoginService {
    LoginResponse login(WardLoginRequest request);
}
