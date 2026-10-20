package com.unify.onyak.domain.users.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.profile.dto.response.MyProfileResponse;
import com.unify.onyak.domain.users.profile.error.exception.UserNotFound;
import com.unify.onyak.domain.users.profile.service.MyProfileService;
import com.unify.onyak.global.error.CustomException;
import com.unify.onyak.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class MyProfileServiceImpl implements MyProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFound::new);

        return MyProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .emailVerified(user.getEmailVerified())
                .age(user.getBirthDate() == null ? null
                        : Period.between(user.getBirthDate(), LocalDate.now()).getYears())
                .weight(user.getWeight())
                .gender(user.getGender() == null ? null : user.getGender().name())
                .isPregnant(user.getIsPregnant())
                .build();
    }
}