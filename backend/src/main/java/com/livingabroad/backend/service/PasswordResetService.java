package com.livingabroad.backend.service;

import com.livingabroad.backend.entity.PasswordResetToken;
import com.livingabroad.backend.entity.User;
import com.livingabroad.backend.exception.InvalidPasswordResetTokenException;
import com.livingabroad.backend.repository.PasswordResetTokenRepository;
import com.livingabroad.backend.repository.RefreshTokenRepository;
import com.livingabroad.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.expiration-ms}")
    private long expirationMs;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public PasswordResetService(
        PasswordResetTokenRepository tokenRepository,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        MailService mailService,
        PasswordEncoder passwordEncoder
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    // 계정 존재 여부를 이메일 유무로 알 수 없도록(enumeration 방지), 가입되지 않은 이메일이거나
    // 메일 발송이 실패해도 예외를 던지지 않는다 — 컨트롤러는 항상 동일한 204를 반환한다.
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            try {
                issueAndSendResetToken(user);
            } catch (Exception e) {
                log.warn("비밀번호 재설정 메일 발송 실패 (userId={})", user.getUserId(), e);
            }
        });
    }

    private void issueAndSendResetToken(User user) {
        tokenRepository.findAllByUserIdAndUsedAtIsNull(user.getUserId())
            .forEach(token -> {
                token.markUsed();
                tokenRepository.save(token);
            });

        String rawToken = generateRawToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(Duration.ofMillis(expirationMs));
        tokenRepository.save(new PasswordResetToken(user.getUserId(), hashToken(rawToken), expiresAt));

        String link = frontendBaseUrl + "/reset-password?token=" + rawToken;
        String body = user.getUserName() + "님, 안녕하세요.\n\n"
            + "비밀번호를 재설정하려면 아래 링크를 클릭해주세요.\n\n"
            + link + "\n\n"
            + "이 링크는 1시간 동안 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.";
        mailService.send(user.getEmail(), "[Living Abroad] 비밀번호 재설정", body);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hashToken(rawToken))
            .orElseThrow(InvalidPasswordResetTokenException::new);

        if (!token.isUsable()) {
            throw new InvalidPasswordResetTokenException();
        }

        token.markUsed();

        User user = userRepository.findById(token.getUserId())
            .orElseThrow(InvalidPasswordResetTokenException::new);
        user.changePassword(passwordEncoder.encode(newPassword));

        // 재설정 전 이미 로그인돼 있던 다른 세션들도 전부 무효화한다 (계정 복구 시나리오의 표준 보안 관행).
        refreshTokenRepository.revokeAllByUserId(user.getUserId(), OffsetDateTime.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
