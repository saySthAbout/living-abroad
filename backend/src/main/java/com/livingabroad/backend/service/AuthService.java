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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.name());
        User saved = userRepository.save(user);
        return buildResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(rawRefreshToken))
            .orElseThrow(InvalidRefreshTokenException::new);

        if (!stored.isUsable()) {
            throw new InvalidRefreshTokenException();
        }

        stored.revoke();

        User user = userRepository.findById(stored.getUserId())
            .orElseThrow(InvalidRefreshTokenException::new);

        return buildResponse(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(rawRefreshToken))
            .ifPresent(RefreshToken::revoke);
    }

    private AuthResponse buildResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String rawRefreshToken = jwtService.generateRawRefreshToken();
        String refreshTokenHash = jwtService.hashRefreshToken(rawRefreshToken);
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(Duration.ofMillis(jwtService.refreshExpirationMs()));

        refreshTokenRepository.save(new RefreshToken(user.getUserId(), refreshTokenHash, expiresAt));

        return new AuthResponse(
            accessToken,
            "Bearer",
            jwtService.expirationSeconds(),
            rawRefreshToken,
            new AuthResponse.UserSummary(user.getUserId(), user.getUserName(), user.getEmail())
        );
    }
}
