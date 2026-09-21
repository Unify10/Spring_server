package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void logout(Long userId, String role) {
        String pattern = "refresh:" + role + ":" + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}