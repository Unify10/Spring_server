package com.unify.onyak.global.jwt;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.global.error.CustomException;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String EXCEPTION_ATTRIBUTE = "jwtException";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            try {
                authenticate(token);
            } catch (CustomException e) {
                // 필터는 @RestControllerAdvice 보다 앞이라 직접 처리 못 함 → EntryPoint 가 401 응답하도록 넘김
                request.setAttribute(EXCEPTION_ATTRIBUTE, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        jwtProvider.validateToken(token, "access");

        Long userId = jwtProvider.getUserId(token);
        Role role = jwtProvider.getRole(token);

        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                String.valueOf(userId),
                null,
                "ROLE_" + role.name()
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
