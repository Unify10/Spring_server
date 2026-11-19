package com.unify.onyak.domain.users.auth;

import com.unify.onyak.domain.users.auth.controller.AuthController;
import com.unify.onyak.domain.users.auth.dto.request.LoginRequest;
import com.unify.onyak.domain.users.auth.dto.request.SignUpRequest;
import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.dto.response.SignUpResponse;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.policy.UserValidationPolicy;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.users.auth.service.EmailVerificationService;
import com.unify.onyak.domain.users.auth.service.GoogleLoginService;
import com.unify.onyak.domain.users.auth.service.UserSignUpService;
import com.unify.onyak.domain.users.auth.service.impl.AuthServiceImpl;
import com.unify.onyak.domain.users.auth.service.impl.UserSignUpServiceImpl;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 로그인·가입 응답 형식(이슈 #30). DB·Redis 없이 돈다. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class AuthResponseTest {

    private static final String SECRET = "dGVzdC1vbmx5LXNlY3JldC1rZXktbm90LWZvci1wcm9kdWN0aW9uLXVzZS0xMjM0NTY3OA==";

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProviderBean;
    @MockitoBean AuthService authService;
    @MockitoBean UserSignUpService userSignUpService;
    @MockitoBean EmailVerificationService emailVerificationService;
    @MockitoBean GoogleLoginService googleLoginService;

    @Test
    void 로그인_응답은_사용자_정보_role_토큰을_snake_case로_내려준다() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse(7L, "a@a.com", "홍길동", Role.GUARDIAN, "access", "refresh"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@a.com\",\"password\":\"password1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(7))
                .andExpect(jsonPath("$.data.email").value("a@a.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.role").value("GUARDIAN"))
                .andExpect(jsonPath("$.data.access_token").value("access"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh"))
                .andExpect(jsonPath("$.data.length()").value(6));
    }

    @Test
    void 가입_응답은_201이고_토큰을_포함하며_role은_없다() throws Exception {
        when(userSignUpService.signUp(any(SignUpRequest.class)))
                .thenReturn(new SignUpResponse(7L, "a@a.com", "홍길동", "access", "refresh"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "a@a.com",
                                  "password": "password1234",
                                  "password_confirm": "password1234",
                                  "name": "홍길동",
                                  "phone": "010-1234-5678"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user_id").value(7))
                .andExpect(jsonPath("$.data.email").value("a@a.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.access_token").value("access"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh"))
                .andExpect(jsonPath("$.data.role").doesNotExist())
                .andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    void 서비스_로그인은_GUARDIAN_토큰을_발급하고_Redis에_저장한다() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        UserValidationPolicy policy = mock(UserValidationPolicy.class);
        JwtProvider jwtProvider = new JwtProvider(SECRET, 3_600_000L, 7_200_000L);
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("a@a.com");
        when(user.getName()).thenReturn("홍길동");
        when(policy.validateLogin("a@a.com", "password1234")).thenReturn(user);

        LoginResponse response = new AuthServiceImpl(jwtProvider, policy, repository)
                .login(new LoginRequest("a@a.com", "password1234"));

        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.role()).isEqualTo(Role.GUARDIAN);
        assertThat(jwtProvider.getRole(response.accessToken())).isEqualTo(Role.GUARDIAN);
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo("GUARDIAN:7");
        assertThat(saved.getValue().getRefreshToken()).isEqualTo(response.refreshToken());
    }

    @Test
    void 서비스_가입은_저장한_사용자로_GUARDIAN_토큰을_발급해_응답에_담는다() {
        UserRepository userRepository = mock(UserRepository.class);
        UserValidationPolicy policy = mock(UserValidationPolicy.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService auth = mock(AuthService.class);
        when(passwordEncoder.encode("password1234")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });
        when(auth.issueTokens(7L, Role.GUARDIAN)).thenReturn(new TokenResponse("access", "refresh"));

        SignUpResponse response = new UserSignUpServiceImpl(userRepository, policy, passwordEncoder, auth)
                .signUp(new SignUpRequest("a@a.com", "password1234", "password1234", "홍길동", "010-1234-5678",
                        LocalDate.of(2000, 1, 1), 60.5f, 170.0f, Gender.M, null, null, null, null));

        verify(auth).issueTokens(eq(7L), eq(Role.GUARDIAN));
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.email()).isEqualTo("a@a.com");
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void 서비스_가입은_isPregnant_값을_저장한다() {
        UserRepository userRepository = mock(UserRepository.class);
        UserValidationPolicy policy = mock(UserValidationPolicy.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService auth = mock(AuthService.class);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(auth.issueTokens(any(), any())).thenReturn(new TokenResponse("access", "refresh"));

        UserSignUpServiceImpl service = new UserSignUpServiceImpl(userRepository, policy, passwordEncoder, auth);

        // 1. isPregnant = true 인 경우
        SignUpRequest requestWithPregnant = new SignUpRequest("a@a.com", "password1234", "password1234", "홍길동", "010-1234-5678",
                LocalDate.of(2000, 1, 1), 60.5f, 170.0f, Gender.F, true, null, null, null);
        service.signUp(requestWithPregnant);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getIsPregnant()).isTrue();

        // 2. isPregnant = null 인 경우
        SignUpRequest requestWithoutPregnant = new SignUpRequest("b@b.com", "password1234", "password1234", "홍길동", "010-1234-5678",
                LocalDate.of(2000, 1, 1), 60.5f, 170.0f, Gender.M, null, null, null, null);
        service.signUp(requestWithoutPregnant);

        verify(userRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getValue().getIsPregnant()).isNull();
    }
}
