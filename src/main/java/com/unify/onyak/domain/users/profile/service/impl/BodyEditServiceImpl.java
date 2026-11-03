package com.unify.onyak.domain.users.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.profile.dto.request.BodyEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.BodyEditResponse;
import com.unify.onyak.domain.users.profile.error.exception.InvalidPregnancy;
import com.unify.onyak.domain.users.profile.error.exception.UserForbidden;
import com.unify.onyak.domain.users.profile.error.exception.UserNotFound;
import com.unify.onyak.domain.users.profile.service.BodyEditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BodyEditServiceImpl implements BodyEditService {

    private final UserRepository userRepository;

    @Transactional
    @Override
    public BodyEditResponse editBody(Long userId, Role role, BodyEditRequest request) {
        if (role != Role.GUARDIAN) {
            throw new UserForbidden();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFound::new);

        Gender gender = request.gender() != null ? request.gender() : user.getGender();
        Boolean isPregnant = request.isPregnant();

        if (gender == Gender.M) {
            if (Boolean.TRUE.equals(isPregnant)) {
                throw new InvalidPregnancy();
            }
            // 요청에 임신 여부가 없어도 최종 성별이 남성이면 기존 true를 정리
            if (Boolean.TRUE.equals(user.getIsPregnant())) {
                isPregnant = false;
            }
        }

        user.updateBody(request.birthDate(), request.weight(), request.height(),
                request.gender(), isPregnant);

        return BodyEditResponse.from(user);
    }
}
