package com.livingabroad.backend.service;

import com.livingabroad.backend.entity.EmailVerificationToken;
import com.livingabroad.backend.entity.User;
import com.livingabroad.backend.exception.EmailAlreadyVerifiedException;
import com.livingabroad.backend.exception.InvalidVerificationTokenException;
import com.livingabroad.backend.repository.EmailVerificationTokenRepository;
import com.livingabroad.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        setField("expirationMs", 86_400_000L);
        setField("frontendBaseUrl", "http://localhost:5173");
    }

    private void setField(String name, Object value) {
        try {
            Field field = EmailVerificationService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(emailVerificationService, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static EmailVerificationToken tokenFor(Long userId, String hash, OffsetDateTime expiresAt) {
        try {
            EmailVerificationToken token = new EmailVerificationToken(userId, hash, expiresAt);
            Field idField = EmailVerificationToken.class.getDeclaredField("tokenId");
            idField.setAccessible(true);
            idField.set(token, 1L);
            return token;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendVerificationEmailInvalidatesOldTokensAndSendsMail() {
        User user = new User("user@example.com", "hashed", "User");
        setUserId(user, 7L);
        EmailVerificationToken stalePending = tokenFor(7L, "old-hash", OffsetDateTime.now().plusHours(1));
        lenient().when(tokenRepository.findAllByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of(stalePending));

        emailVerificationService.sendVerificationEmail(user);

        assertThat(stalePending.isUsable()).isFalse();
        verify(tokenRepository, times(2)).save(any(EmailVerificationToken.class));
        verify(mailService).send(eq("user@example.com"), anyString(), anyString());
    }

    @Test
    void verifyMarksTokenUsedAndUserVerified() {
        User user = new User("user@example.com", "hashed", "User");
        setUserId(user, 7L);
        EmailVerificationToken token = tokenFor(7L, "raw-token-hash", OffsetDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        emailVerificationService.verify("raw-token");

        assertThat(token.isUsable()).isFalse();
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void verifyRejectsUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verify("bogus-token"))
            .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void verifyRejectsExpiredToken() {
        EmailVerificationToken token = tokenFor(7L, "expired-hash", OffsetDateTime.now().minusHours(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> emailVerificationService.verify("expired-token"))
            .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void verifyRejectsAlreadyUsedToken() {
        EmailVerificationToken token = tokenFor(7L, "used-hash", OffsetDateTime.now().plusHours(1));
        token.markUsed();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> emailVerificationService.verify("used-token"))
            .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void resendRejectsAlreadyVerifiedUser() {
        User user = new User("user@example.com", "hashed", "User");
        setUserId(user, 7L);
        user.markEmailVerified();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> emailVerificationService.resend(7L))
            .isInstanceOf(EmailAlreadyVerifiedException.class);

        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void resendSendsNewEmailForUnverifiedUser() {
        User user = new User("user@example.com", "hashed", "User");
        setUserId(user, 7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        lenient().when(tokenRepository.findAllByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of());

        emailVerificationService.resend(7L);

        verify(mailService).send(eq("user@example.com"), anyString(), anyString());
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
}
