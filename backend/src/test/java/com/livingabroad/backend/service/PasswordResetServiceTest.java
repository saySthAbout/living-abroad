package com.livingabroad.backend.service;

import com.livingabroad.backend.entity.PasswordResetToken;
import com.livingabroad.backend.entity.User;
import com.livingabroad.backend.exception.InvalidPasswordResetTokenException;
import com.livingabroad.backend.repository.PasswordResetTokenRepository;
import com.livingabroad.backend.repository.RefreshTokenRepository;
import com.livingabroad.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        setField("expirationMs", 3_600_000L);
        setField("frontendBaseUrl", "http://localhost:5173");
    }

    private void setField(String name, Object value) {
        try {
            Field field = PasswordResetService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(passwordResetService, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static PasswordResetToken tokenFor(Long userId, String hash, OffsetDateTime expiresAt) {
        return new PasswordResetToken(userId, hash, expiresAt);
    }

    private static void setUserId(User user, Long userId) {
        try {
            Field field = User.class.getDeclaredField("userId");
            field.setAccessible(true);
            field.set(user, userId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void requestResetInvalidatesOldTokensAndSendsMailForKnownEmail() {
        User user = new User("user@example.com", "hashed", "User");
        setUserId(user, 7L);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        PasswordResetToken stalePending = tokenFor(7L, "old-hash", OffsetDateTime.now().plusHours(1));
        lenient().when(tokenRepository.findAllByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of(stalePending));

        passwordResetService.requestReset("user@example.com");

        assertThat(stalePending.isUsable()).isFalse();
        verify(tokenRepository, times(2)).save(any(PasswordResetToken.class));
        verify(mailService).send(eq("user@example.com"), anyString(), anyString());
    }

    @Test
    void requestResetDoesNothingForUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("nobody@example.com");

        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void requestResetDoesNotThrowWhenMailSendingFails() {
        User user = new User("user@example.com", "hashed", "User");
        setUserId(user, 7L);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        lenient().when(tokenRepository.findAllByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of());
        lenient().doThrow(new RuntimeException("smtp down")).when(mailService).send(anyString(), anyString(), anyString());

        assertThat(catchThrowableOrNull(() -> passwordResetService.requestReset("user@example.com"))).isNull();
    }

    private static Throwable catchThrowableOrNull(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    @Test
    void resetPasswordUpdatesPasswordAndRevokesAllSessions() {
        User user = new User("user@example.com", "old-hash", "User");
        setUserId(user, 7L);
        PasswordResetToken token = tokenFor(7L, "raw-token-hash", OffsetDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");

        passwordResetService.resetPassword("raw-token", "newPassword1");

        assertThat(token.isUsable()).isFalse();
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenRepository).revokeAllByUserId(eq(7L), any(OffsetDateTime.class));
    }

    @Test
    void resetPasswordRejectsUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("bogus-token", "newPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
        verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong(), any());
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        PasswordResetToken token = tokenFor(7L, "expired-hash", OffsetDateTime.now().minusHours(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "newPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void resetPasswordRejectsAlreadyUsedToken() {
        PasswordResetToken token = tokenFor(7L, "used-hash", OffsetDateTime.now().plusHours(1));
        token.markUsed();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", "newPassword1"))
            .isInstanceOf(InvalidPasswordResetTokenException.class);
    }
}
