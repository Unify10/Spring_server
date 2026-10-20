package com.unify.onyak.domain.users.withdraw.service;

import com.unify.onyak.domain.users.auth.entity.Role;

public interface WithdrawService {
    void withdraw(Long userId, Role role);
}