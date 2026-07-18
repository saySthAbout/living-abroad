package com.livingabroad.backend.service;

import com.livingabroad.backend.dto.auth.AuthResponse;
import com.livingabroad.backend.dto.auth.LoginRequest;
import com.livingabroad.backend.dto.auth.SignupRequest;
import com.livingabroad.backend.entity.User;
import com.livingabroad.backend.exception.EmailAlreadyExistsException;
import com.livingabroad.backend.exception.InvalidCredentialsException;
import com.livingabroad.backend.repository.UserRepository;
import com.livingabroad.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void signupCreatesUserAndReturnsToken() {
        SignupRequest request = new SignupRequest("Test User", "new@example.com", "Passw0rd1");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("token-value");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.signup(request);

        assertThat(response.accessToken()).isEqualTo("token-value");
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
        User user = new User("user@example.com", "hashed", "User");
        LoginRequest request = new LoginRequest("user@example.com", "Passw0rd1");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd1", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token-value");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("token-value");
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
}
