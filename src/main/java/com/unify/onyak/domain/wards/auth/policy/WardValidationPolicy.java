package com.unify.onyak.domain.wards.auth.policy;

import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.wards.auth.error.exception.UserIdNotFoundException;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Component
public class WardValidationPolicy {
    private final WardRepository wardRepository;
    private final UserRepository userRepository;

    public void ValidateGuardianId(Long id){

        // 보호자 아이디 존재 여부 체크
        if(!userRepository.existsById(id)){
            throw new UserIdNotFoundException();
        }
    }
}
