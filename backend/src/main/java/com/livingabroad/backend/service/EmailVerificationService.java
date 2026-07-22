package com.livingabroad.backend.service;

import com.livingabroad.backend.entity.EmailVerificationToken;
import com.livingabroad.backend.entity.User;
import com.livingabroad.backend.exception.EmailAlreadyVerifiedException;
import com.livingabroad.backend.exception.InvalidVerificationTokenException;
import com.livingabroad.backend.exception.UserNotFoundException;
import com.livingabroad.backend.repository.EmailVerificationTokenRepository;
import com.livingabroad.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
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
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.email-verification.expiration-ms}")
    private long expirationMs;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public EmailVerificationService(
        EmailVerificationTokenRepository tokenRepository,
        UserRepository userRepository,
        MailService mailService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    // 회원가입 트랜잭션 안에서 호출되므로, 메일 발송(외부 I/O) 실패가 가입 자체를 롤백시키지 않도록
    // 일부러 @Transactional을 붙이지 않는다 — 각 save() 호출이 개별적으로 커밋된다.
    public void sendVerificationEmail(User user) {
        tokenRepository.findAllByUserIdAndUsedAtIsNull(user.getUserId())
            .forEach(token -> {
                token.markUsed();
                tokenRepository.save(token);
            });

        String rawToken = generateRawToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(Duration.ofMillis(expirationMs));
        tokenRepository.save(new EmailVerificationToken(user.getUserId(), hashToken(rawToken), expiresAt));

        String link = frontendBaseUrl + "/verify-email?token=" + rawToken;
        String body = user.getUserName() + "님, 안녕하세요.\n\n"
            + "Living Abroad 이메일 인증을 완료하려면 아래 링크를 클릭해주세요.\n\n"
            + link + "\n\n"
            + "이 링크는 24시간 동안 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.";
        mailService.send(user.getEmail(), "[Living Abroad] 이메일 인증을 완료해주세요", body);
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hashToken(rawToken))
            .orElseThrow(InvalidVerificationTokenException::new);

        if (!token.isUsable()) {
            throw new InvalidVerificationTokenException();
        }

        token.markUsed();

        User user = userRepository.findById(token.getUserId())
            .orElseThrow(InvalidVerificationTokenException::new);
        user.markEmailVerified();
    }

    public void resend(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException();
        }
        sendVerificationEmail(user);
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
