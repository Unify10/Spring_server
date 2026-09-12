package com.unify.onyak.domain.users.auth.service;

import com.unify.onyak.domain.users.auth.dto.request.SignUpRequest;
import com.unify.onyak.domain.users.auth.dto.response.SignUpResponse;

public interface UserSignUpService {
    SignUpResponse signUp(SignUpRequest request);
}
