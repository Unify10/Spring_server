package com.unify.onyak.domain.auth.users.repository;

import com.unify.onyak.domain.auth.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String Email);
}
