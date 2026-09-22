package com.unify.onyak.domain.users.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "refresh")
public class RefreshToken {

    @Id
    private Long userId;

    private Role role;

    private String refreshToken;

    @TimeToLive(unit = TimeUnit.MILLISECONDS)// Redis에서 만료 시간 설정에 사용될 단일 숫자형 속성을 지정
    private long ttl;
}
