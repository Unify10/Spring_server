package com.unify.onyak.domain.users.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

// 가입용 email_verification 과 키를 분리해 서로 덮어쓰지 않게 한다 (password_reset:{email})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "password_reset", timeToLive = 300)
public class PasswordReset {

    @Id
    private String email;

    private String code;

    private boolean verified;

    // 코드 틀린 횟수. 한도를 넘으면 저장값을 지워 재발송을 강제한다
    private int failedAttempts;

    public static PasswordReset issue(String email, String code) {
        return new PasswordReset(email, code, false, 0);
    }

    public PasswordReset withFailedAttempt() {
        return new PasswordReset(email, code, verified, failedAttempts + 1);
    }

    public PasswordReset markVerified() {
        return new PasswordReset(email, code, true, failedAttempts);
    }
}
