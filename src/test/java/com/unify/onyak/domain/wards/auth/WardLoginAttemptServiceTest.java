package com.unify.onyak.domain.wards.auth;

import com.unify.onyak.domain.wards.auth.service.impl.WardLoginAttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WardLoginAttemptServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private WardLoginAttemptServiceImpl wardLoginAttemptService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        wardLoginAttemptService = new WardLoginAttemptServiceImpl(redisTemplate);
    }

    @Test
    void 첫_실패_시_횟수를_증가시키고_5분_TTL을_설정한다() {
        when(valueOperations.increment("ward:login:attempt:127.0.0.1"))
                .thenReturn(1L);

        wardLoginAttemptService.recordFailure("127.0.0.1");

        verify(valueOperations).increment("ward:login:attempt:127.0.0.1");
        verify(redisTemplate).expire(
                "ward:login:attempt:127.0.0.1",
                Duration.ofMinutes(5)
        );
    }

    @Test
    void 실패횟수가_5회_이상이면_IP를_차단한다() {
        when(valueOperations.get("ward:login:attempt:127.0.0.1"))
                .thenReturn("5");

        assertTrue(wardLoginAttemptService.isBlocked("127.0.0.1"));
    }

    @Test
    void 실패횟수가_5회_미만이면_IP를_차단하지_않는다() {
        when(valueOperations.get("ward:login:attempt:127.0.0.1"))
                .thenReturn("4");

        assertFalse(wardLoginAttemptService.isBlocked("127.0.0.1"));
    }

}
