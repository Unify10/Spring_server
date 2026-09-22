package com.unify.onyak.domain.users.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "email_verification", timeToLive = 300)
public class EmailVerification {

    @Id
    private String email;

    @Indexed
    private String code;

    private boolean verified;
}
