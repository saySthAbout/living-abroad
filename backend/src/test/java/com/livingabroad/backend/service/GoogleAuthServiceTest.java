package com.livingabroad.backend.service;

import com.livingabroad.backend.exception.InvalidGoogleTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private JwtDecoder googleIdTokenDecoder;

    private GoogleAuthService googleAuthService;

    private Jwt jwtWithClaims(Map<String, Object> claims) {
        return Jwt.withTokenValue("raw-token")
            .header("alg", "RS256")
            .claims(c -> c.putAll(claims))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    @Test
    void verifyReturnsIdentityForValidToken() {
        googleAuthService = new GoogleAuthService(googleIdTokenDecoder);
        when(googleIdTokenDecoder.decode("valid-token")).thenReturn(jwtWithClaims(Map.of(
            "sub", "sub-1",
            "email", "user@example.com",
            "email_verified", true,
            "name", "User Name"
        )));

        GoogleAuthService.GoogleIdentity identity = googleAuthService.verify("valid-token");

        assertThat(identity.sub()).isEqualTo("sub-1");
        assertThat(identity.email()).isEqualTo("user@example.com");
        assertThat(identity.name()).isEqualTo("User Name");
    }

    @Test
    void verifyFallsBackToEmailWhenNameMissing() {
        googleAuthService = new GoogleAuthService(googleIdTokenDecoder);
        when(googleIdTokenDecoder.decode("valid-token")).thenReturn(jwtWithClaims(Map.of(
            "sub", "sub-1",
            "email", "user@example.com",
            "email_verified", true
        )));

        GoogleAuthService.GoogleIdentity identity = googleAuthService.verify("valid-token");

        assertThat(identity.name()).isEqualTo("user@example.com");
    }

    @Test
    void verifyRejectsUnverifiedEmail() {
        googleAuthService = new GoogleAuthService(googleIdTokenDecoder);
        when(googleIdTokenDecoder.decode("unverified-token")).thenReturn(jwtWithClaims(Map.of(
            "sub", "sub-1",
            "email", "user@example.com",
            "email_verified", false
        )));

        assertThatThrownBy(() -> googleAuthService.verify("unverified-token"))
            .isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void verifyRejectsWhenDecoderThrows() {
        googleAuthService = new GoogleAuthService(googleIdTokenDecoder);
        when(googleIdTokenDecoder.decode("bad-token")).thenThrow(new JwtException("bad signature"));

        assertThatThrownBy(() -> googleAuthService.verify("bad-token"))
            .isInstanceOf(InvalidGoogleTokenException.class);
    }
}
