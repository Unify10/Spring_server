package com.unify.onyak.domain.users.profile.service;

import com.unify.onyak.domain.users.profile.dto.request.MyProfileEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.MyProfileEditResponse;

public interface MyProfileEditService {
    MyProfileEditResponse editMyProfile(long userId, MyProfileEditRequest request);
}
