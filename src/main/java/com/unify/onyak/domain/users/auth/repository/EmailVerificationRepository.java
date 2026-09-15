package com.unify.onyak.domain.users.auth.repository;

import com.unify.onyak.domain.users.auth.entity.EmailVerification;
import org.springframework.data.repository.CrudRepository;

public interface EmailVerificationRepository extends CrudRepository<EmailVerification, String> {

    boolean existsByEmailAndCode(String email, String code);
}
