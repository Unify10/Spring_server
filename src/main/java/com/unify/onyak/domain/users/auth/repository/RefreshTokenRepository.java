package com.unify.onyak.domain.users.auth.repository;

import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
}
