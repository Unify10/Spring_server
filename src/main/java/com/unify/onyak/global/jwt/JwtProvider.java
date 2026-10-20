package com.unify.onyak.global.jwt;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.error.exception.InvalidTokenException;
import com.unify.onyak.domain.users.auth.error.exception.TokenExpiredException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Getter
@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final Long accessTokenExpTime;
    private final Long refreshTokenExpTime;

    public JwtProvider(
            @Value("${jwt.secret}")
            String secretKey,

            @Value("${jwt.access-token-validity-in-milliseconds}")
            Long accessTokenExpTime,

            @Value("${jwt.refresh-token-validity-in-milliseconds}")
            Long refreshTokenExpTime
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpTime = accessTokenExpTime;
        this.refreshTokenExpTime = refreshTokenExpTime;
    }

    public String generateAccessToken(Long userId, Role role) {
        return createToken(userId, role, accessTokenExpTime, "access");
    }

    // refreshToken 발급
    public String generateRefreshToken(Long userId, Role role) {
        return createToken(userId, role, refreshTokenExpTime, "refresh");
    }

    private String createToken(Long userId, Role role, Long expiration, String type) {
        Date now = new Date();
        Date expirationTime = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // 같은 초에 발급해도 토큰이 달라지도록 jti 부여
                .subject(String.valueOf(userId))
                .claim("type", type)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expirationTime)
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token) {
        try {
            return Long.parseLong(parseClaims(token).getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidTokenException();
        }
    }

    public Role getRole(String token) {
        String role = parseClaims(token).get("role", String.class);

        if (role == null) {
            throw new InvalidTokenException();
        }

        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException();
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException();
        }
    }

    public void validateToken(String token, String expectedType) {
        Claims claims = parseClaims(token);
        String type = claims.get("type", String.class);

        if (!expectedType.equals(type)) {
            throw new InvalidTokenException();
        }
    }
}
