package com.unify.onyak.domain.users.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
@RedisHash(value = "refresh")
public class RefreshToken {

    // users.id 와 wards.id 가 겹칠 수 있어 role 을 포함한 키 사용 (refresh:{role}:{id})
    @Id
    private String id;

    private Long userId;

    private Role role;

    private String refreshToken;

    @TimeToLive(unit = TimeUnit.MILLISECONDS)// Redis에서 만료 시간 설정에 사용될 단일 숫자형 속성을 지정
    private long ttl;

    public RefreshToken(Long userId, Role role, String refreshToken, long ttl) {
        this.id = keyOf(role, userId);
        this.userId = userId;
        this.role = role;
        this.refreshToken = refreshToken;
        this.ttl = ttl;
    }

    public static String keyOf(Role role, Long userId) {
        return role.name() + ":" + userId;
    }
}
