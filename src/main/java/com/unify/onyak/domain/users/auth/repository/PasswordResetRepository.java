package com.unify.onyak.domain.users.auth.repository;

import com.unify.onyak.domain.users.auth.entity.PasswordReset;
import org.springframework.data.repository.CrudRepository;

// email 이 @Id 이므로 조회는 findById 로만 한다 (파생 쿼리는 보조 색인이 없어 동작하지 않음)
public interface PasswordResetRepository extends CrudRepository<PasswordReset, String> {
}
