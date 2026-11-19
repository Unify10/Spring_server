package com.unify.onyak.domain.users.auth.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.unify.onyak.domain.users.auth.dto.GoogleUserInfo;
import com.unify.onyak.domain.users.auth.error.exception.GoogleTokenInvalidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleIdTokenVerifierImplTest {

    private static final String CLIENT_IDS = "test-client-id";

    @Test
    @DisplayName("클라이언트 ID 목록이 비어있으면 검증 시 GoogleTokenInvalidException 발생")
    void 클라이언트ID_미설정시_검증실패() {
        GoogleIdTokenVerifierImpl verifier = new GoogleIdTokenVerifierImpl("");

        assertThatThrownBy(() -> verifier.verify("some-token"))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    @DisplayName("토큰이 null이거나 공백이면 GoogleTokenInvalidException 발생")
    void 토큰_누락시_검증실패() {
        GoogleIdTokenVerifierImpl verifier = new GoogleIdTokenVerifierImpl(CLIENT_IDS);

        assertThatThrownBy(() -> verifier.verify(null))
                .isInstanceOf(GoogleTokenInvalidException.class);

        assertThatThrownBy(() -> verifier.verify("   "))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    @DisplayName("토큰 길이가 4096자를 초과하면 GoogleTokenInvalidException 발생")
    void 토큰_길이초과시_검증실패() {
        GoogleIdTokenVerifierImpl verifier = new GoogleIdTokenVerifierImpl(CLIENT_IDS);
        String hugeToken = "a".repeat(4097);

        assertThatThrownBy(() -> verifier.verify(hugeToken))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    @DisplayName("서명·만료·issuer·audience 불일치로 verify가 null을 반환하면 GoogleTokenInvalidException 발생")
    void 구글SDK_검증실패_null반환() throws GeneralSecurityException, IOException {
        com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier sdkVerifier =
                mock(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class);
        when(sdkVerifier.verify(anyString())).thenReturn(null);

        GoogleIdTokenVerifierImpl verifier = new GoogleIdTokenVerifierImpl(CLIENT_IDS, sdkVerifier);

        assertThatThrownBy(() -> verifier.verify("invalid-token"))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    @DisplayName("SDK verifier에서 GeneralSecurityException 또는 IOException 발생 시 GoogleTokenInvalidException 발생")
    void 구글SDK_검증예외발생() throws GeneralSecurityException, IOException {
        com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier sdkVerifier =
                mock(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class);
        when(sdkVerifier.verify(anyString())).thenThrow(new GeneralSecurityException("Signature verification failed"));

        GoogleIdTokenVerifierImpl verifier = new GoogleIdTokenVerifierImpl(CLIENT_IDS, sdkVerifier);

        assertThatThrownBy(() -> verifier.verify("invalid-token"))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    @DisplayName("클레임 email_verified가 false이거나 null이면 GoogleTokenInvalidException 발생")
    void email_verified_미인증_실패() throws GeneralSecurityException, IOException {
        com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier sdkVerifier =
                mock(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmailVerified(false);
        payload.setSubject("google-123");
        payload.setEmail("user@example.com");

        when(idToken.getPayload()).thenReturn(payload);
        when(sdkVerifier.verify(anyString())).thenReturn(idToken);

        GoogleIdTokenVerifierImpl verifier = new GoogleIdTokenVerifierImpl(CLIENT_IDS, sdkVerifier);

        assertThatThrownBy(() -> verifier.verify("valid-looking-token"))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    @DisplayName("sub(google_id) 또는 email 누락 시 GoogleTokenInvalidException 발생")
    void 필수클레임_누락_실패() throws GeneralSecurityException, IOException {
        com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier sdkVerifier =
                mock(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmailVerified(true);
        // subject 누락
        payload.setEmail("user@example.com");

        when(idToken.getPayload()).thenReturn(payload);
        when(sdkVerifier.verify(anyString())).thenReturn(idToken);

        GoogleIdTokenVerifierImpl verifier = new GoogleIdTokenVerifierImpl(CLIENT_IDS, sdkVerifier);

        assertThatThrownBy(() -> verifier.verify("valid-looking-token"))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    @DisplayName("검증 성공 시 sub, email, name 정상 추출")
    void 검증성공_이름포함() throws GeneralSecurityException, IOException {
        com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier sdkVerifier =
                mock(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmailVerified(true);
        payload.setSubject("google-999");
        payload.setEmail("onyak@example.com");
        payload.set("name", "온약보호자");

        when(idToken.getPayload()).thenReturn(payload);
        when(sdkVerifier.verify(anyString())).thenReturn(idToken);

        GoogleIdTokenVerifierImpl verifier = new GoogleIdTokenVerifierImpl(CLIENT_IDS, sdkVerifier);
        GoogleUserInfo userInfo = verifier.verify("valid-token");

        assertThat(userInfo.googleId()).isEqualTo("google-999");
        assertThat(userInfo.email()).isEqualTo("onyak@example.com");
        assertThat(userInfo.name()).isEqualTo("온약보호자");
    }

    @Test
    @DisplayName("name 클레임이 없으면 email의 @ 앞부분으로 이름 설정")
    void 검증성공_이름없을때_이메일앞부분사용() throws GeneralSecurityException, IOException {
        com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier sdkVerifier =
                mock(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmailVerified(true);
        payload.setSubject("google-888");
        payload.setEmail("guardian123@example.com");
        // name 없음

        when(idToken.getPayload()).thenReturn(payload);
        when(sdkVerifier.verify(anyString())).thenReturn(idToken);

        GoogleIdTokenVerifierImpl verifier = new GoogleIdTokenVerifierImpl(CLIENT_IDS, sdkVerifier);
        GoogleUserInfo userInfo = verifier.verify("valid-token");

        assertThat(userInfo.googleId()).isEqualTo("google-888");
        assertThat(userInfo.email()).isEqualTo("guardian123@example.com");
        assertThat(userInfo.name()).isEqualTo("guardian123");
    }
}
