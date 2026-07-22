package com.livingabroad.backend.service;

import com.livingabroad.backend.dto.auth.AuthResponse;
import com.livingabroad.backend.dto.auth.LoginRequest;
import com.livingabroad.backend.dto.auth.SignupRequest;
import com.livingabroad.backend.entity.RefreshToken;
import com.livingabroad.backend.entity.User;
import com.livingabroad.backend.exception.EmailAlreadyExistsException;
import com.livingabroad.backend.exception.InvalidCredentialsException;
import com.livingabroad.backend.exception.InvalidRefreshTokenException;
import com.livingabroad.backend.repository.RefreshTokenRepository;
import com.livingabroad.backend.repository.UserRepository;
import com.livingabroad.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private void stubTokenIssuance() {
        lenient().when(jwtService.generateToken(any(User.class))).thenReturn("token-value");
        lenient().when(jwtService.expirationSeconds()).thenReturn(3600L);
        lenient().when(jwtService.generateRawRefreshToken()).thenReturn("raw-refresh-token");
        lenient().when(jwtService.hashRefreshToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        lenient().when(jwtService.refreshExpirationMs()).thenReturn(1_209_600_000L);
    }

    private static RefreshToken refreshTokenFor(Long userId, String hash, OffsetDateTime expiresAt) {
        try {
            RefreshToken token = new RefreshToken(userId, hash, expiresAt);
            Field tokenIdField = RefreshToken.class.getDeclaredField("tokenId");
            tokenIdField.setAccessible(true);
            tokenIdField.set(token, 1L);
            return token;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void signupCreatesUserAndReturnsToken() {
        stubTokenIssuance();
        SignupRequest request = new SignupRequest("Test User", "new@example.com", "Passw0rd1");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.signup(request);

        assertThat(response.accessToken()).isEqualTo("token-value");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(response.user().email()).isEqualTo("new@example.com");
        assertThat(response.user().name()).isEqualTo("Test User");
    }

    @Test
    void signupRejectsDuplicateEmail() {
        SignupRequest request = new SignupRequest("Test User", "dup@example.com", "Passw0rd1");
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
            .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        stubTokenIssuance();
        User user = new User("user@example.com", "hashed", "User");
        LoginRequest request = new LoginRequest("user@example.com", "Passw0rd1");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd1", "hashed")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("token-value");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-token");
    }

    @Test
    void loginFailsWithWrongPassword() {
        User user = new User("user@example.com", "hashed", "User");
        LoginRequest request = new LoginRequest("user@example.com", "wrongpass");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginFailsWhenEmailNotFound() {
        LoginRequest request = new LoginRequest("missing@example.com", "Passw0rd1");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshRotatesTokenAndReturnsNewPair() {
        stubTokenIssuance();
        when(jwtService.hashRefreshToken("old-raw-token")).thenReturn("old-hash");
        RefreshToken stored = refreshTokenFor(42L, "old-hash", OffsetDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(stored));
        User user = new User("user@example.com", "hashed", "User");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        AuthResponse response = authService.refresh("old-raw-token");

        assertThat(response.accessToken()).isEqualTo("token-value");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(stored.isUsable()).isFalse();
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(jwtService.hashRefreshToken("bogus-token")).thenReturn("bogus-hash");
        when(refreshTokenRepository.findByTokenHash("bogus-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("bogus-token"))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsExpiredToken() {
        when(jwtService.hashRefreshToken("expired-raw-token")).thenReturn("expired-hash");
        RefreshToken stored = refreshTokenFor(42L, "expired-hash", OffsetDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByTokenHash("expired-hash")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("expired-raw-token"))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutRevokesMatchingToken() {
        when(jwtService.hashRefreshToken("raw-token")).thenReturn("hash");
        RefreshToken stored = refreshTokenFor(42L, "hash", OffsetDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));

        authService.logout("raw-token");

        assertThat(stored.isUsable()).isFalse();
    }

    @Test
    void logoutIsNoOpWhenTokenNotFound() {
        when(jwtService.hashRefreshToken("unknown-token")).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

        authService.logout("unknown-token");
    }
}
