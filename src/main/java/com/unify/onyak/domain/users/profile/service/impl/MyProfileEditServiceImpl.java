package com.unify.onyak.domain.users.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.profile.dto.request.MyProfileEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.MyProfileEditResponse;
import com.unify.onyak.domain.users.profile.error.exception.CurrentPasswordMismatch;
import com.unify.onyak.domain.users.profile.error.exception.UserForbidden;
import com.unify.onyak.domain.users.profile.error.exception.UserNotFound;
import com.unify.onyak.domain.users.profile.service.MyProfileEditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class MyProfileEditServiceImpl implements MyProfileEditService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public MyProfileEditResponse editMyProfile(long userId, Role role, MyProfileEditRequest request) {
        if(role != Role.GUARDIAN) throw new UserForbidden();

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFound::new);

        if(request.password() != null) {
            if(user.getPasswordHash() == null
            || request.currentPassword() == null
            || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())){
                throw new CurrentPasswordMismatch();
            }
        }

        String passwordHash = request.password() == null
                ? null
                : passwordEncoder.encode(request.password());

        user.updateInformation(
                request.phone(),
                passwordHash
        );

        return MyProfileEditResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }
}
