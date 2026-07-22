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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.name());
        User saved = userRepository.save(user);

        // 인증 메일 발송 실패는 회원가입 자체를 막지 않는다 — 사용자는 로그인 후 배너의 "재전송"으로 다시 시도할 수 있다.
        try {
            emailVerificationService.sendVerificationEmail(saved);
        } catch (Exception e) {
            log.warn("인증 메일 발송 실패 (userId={})", saved.getUserId(), e);
        }

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
            new AuthResponse.UserSummary(user.getUserId(), user.getUserName(), user.getEmail(), user.isEmailVerified())
        );
    }
}
