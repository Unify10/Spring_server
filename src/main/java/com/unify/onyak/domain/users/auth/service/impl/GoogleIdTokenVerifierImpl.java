package com.unify.onyak.domain.users.auth.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.unify.onyak.domain.users.auth.dto.GoogleUserInfo;
import com.unify.onyak.domain.users.auth.error.exception.GoogleTokenInvalidException;
import com.unify.onyak.domain.users.auth.service.GoogleIdTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class GoogleIdTokenVerifierImpl implements GoogleIdTokenVerifier {

    public static final int MAX_TOKEN_LENGTH = 4096;

    private final List<String> clientIds;

    private final com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier googleVerifier;

    public GoogleIdTokenVerifierImpl(@Value("${google.client-ids:}") String clientIdsProperty) {
        this(clientIdsProperty, null);
    }

    public GoogleIdTokenVerifierImpl(String clientIdsProperty,
                                     com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier customVerifier) {
        if (clientIdsProperty == null || clientIdsProperty.trim().isEmpty()) {
            this.clientIds = List.of();
        } else {
            this.clientIds = Arrays.stream(clientIdsProperty.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        if (customVerifier != null) {
            this.googleVerifier = customVerifier;
        } else if (!this.clientIds.isEmpty()) {
            this.googleVerifier = new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(this.clientIds)
                    .setIssuers(Arrays.asList("accounts.google.com", "https://accounts.google.com"))
                    .build();
        } else {
            this.googleVerifier = null;
        }
    }

    @Override
    public GoogleUserInfo verify(String idTokenString) {
        if (clientIds.isEmpty() || idTokenString == null || idTokenString.isBlank() || idTokenString.length() > MAX_TOKEN_LENGTH) {
            throw new GoogleTokenInvalidException();
        }
        if (googleVerifier == null) {
            throw new GoogleTokenInvalidException();
        }

        try {
            GoogleIdToken idToken = googleVerifier.verify(idTokenString);
            if (idToken == null) {
                throw new GoogleTokenInvalidException();
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (payload == null) {
                throw new GoogleTokenInvalidException();
            }

            Boolean emailVerified = payload.getEmailVerified();
            if (!Boolean.TRUE.equals(emailVerified)) {
                throw new GoogleTokenInvalidException();
            }

            String googleId = payload.getSubject();
            String email = payload.getEmail();
            if (googleId == null || googleId.isBlank() || email == null || email.isBlank()) {
                throw new GoogleTokenInvalidException();
            }

            String name = (String) payload.get("name");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            return new GoogleUserInfo(googleId, email, name);
        } catch (GoogleTokenInvalidException e) {
            throw e;
        } catch (Exception e) {
            throw new GoogleTokenInvalidException();
        }
    }
}
