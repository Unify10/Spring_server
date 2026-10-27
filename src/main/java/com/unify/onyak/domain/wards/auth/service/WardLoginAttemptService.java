package com.unify.onyak.domain.wards.auth.service;

public interface WardLoginAttemptService {
    void recordFailure(String ip);
    boolean isBlocked(String ip);
}
