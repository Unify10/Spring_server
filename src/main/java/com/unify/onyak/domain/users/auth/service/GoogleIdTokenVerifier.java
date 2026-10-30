package com.unify.onyak.domain.users.auth.service;

import com.unify.onyak.domain.users.auth.dto.GoogleUserInfo;

public interface GoogleIdTokenVerifier {
    GoogleUserInfo verify(String idToken);
}
