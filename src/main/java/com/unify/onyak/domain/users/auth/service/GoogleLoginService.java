package com.unify.onyak.domain.users.auth.service;

import com.unify.onyak.domain.users.auth.dto.request.GoogleLoginRequest;
import com.unify.onyak.domain.users.auth.dto.response.GoogleLoginResponse;

public interface GoogleLoginService {
    GoogleLoginResponse login(GoogleLoginRequest request);
}
