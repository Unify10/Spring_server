package com.unify.onyak.global.config;

import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtAuthenticationFilter;
import com.unify.onyak.global.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        return request -> {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(Collections.singletonList(allowedOrigin)); // 허용할 도메인
            configuration.setAllowedMethods(Collections.singletonList("*")); // 모든 HTTP 메서드 허용
            configuration.setAllowCredentials(true); // 인증 정보 포함 허용
            configuration.setAllowedHeaders(Collections.singletonList("*")); // 모든 헤더 허용
            configuration.setExposedHeaders(Collections.singletonList("Authorization")); // Authorization 헤더 노출
            configuration.setMaxAge(3600L); // 1시간 동안 캐싱

            return configuration;
        };
    }
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            JwtProvider jwtProvider,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())// form login 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/logout").authenticated() // /auth/** 보다 먼저 선언해야 적용됨
                        .requestMatchers("/auth/**", "/app/info")// /auth/** 이외 메서드 전부 인증 필요
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                // Bean 으로 등록하면 서블릿 필터로 중복 등록되므로 체인 안에서만 생성
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
