package com.unify.onyak.domain.wards.auth.service.impl;

import com.unify.onyak.domain.wards.auth.service.WardLoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class WardLoginAttemptServiceImpl implements WardLoginAttemptService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void recordFailure(String ip){
            Long count = redisTemplate.opsForValue().increment(keyOf(ip));
            // 특정 ip 실패 횟수 redis에 저장

            if(count != null && count == 1L){
                redisTemplate.expire(keyOf(ip), Duration.ofMinutes(5));
                // 첫 실패 시점 부터 5분 뒤에 redis에서 자동 삭제
            }
    }

    @Override
    public boolean isBlocked(String ip){
            String count = redisTemplate.opsForValue().get(keyOf(ip));
            return count != null && Long.parseLong(count) >= 5; // null이 아니고 시도 횟수 5회 이상이면 true 반환
    }

    private String keyOf(String ip){
        return "ward:login:attempt:"+ip;
    }
}
