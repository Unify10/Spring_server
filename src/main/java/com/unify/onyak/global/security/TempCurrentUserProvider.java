package com.unify.onyak.global.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// JWT 필터가 없어서 쓰는 임시 클래스입니다. 해당 기능을 완성하면 이 클래스를 지우고 SecurityContext에서 userId를 꺼내는 구현체로 교체해 주세요
@Component
public class TempCurrentUserProvider implements CurrentUserProvider {
    @Override
    public Long getCurrentUserId() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String header = request.getHeader("X-USER-ID");
        if (header == null || header.isBlank()) {
            return 4L; // 헤더가 없으면 1번 유저로 간주
        }
        return Long.parseLong(header);
    }
}
