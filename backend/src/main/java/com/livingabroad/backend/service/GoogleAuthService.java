package com.livingabroad.backend.service;

import com.livingabroad.backend.exception.InvalidGoogleTokenException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    private final JwtDecoder googleIdTokenDecoder;

    public GoogleAuthService(@Qualifier("googleIdTokenDecoder") JwtDecoder googleIdTokenDecoder) {
        this.googleIdTokenDecoder = googleIdTokenDecoder;
    }

    // 서명·발급자·대상(aud)·만료 검증까지 끝난 ID 토큰에서 신원 정보만 뽑아낸다.
    // email_verified가 아니면 Google 쪽에서도 실제 소유가 확인되지 않은 주소이므로 거부한다.
    public GoogleIdentity verify(String idToken) {
        Jwt jwt;
        try {
            jwt = googleIdTokenDecoder.decode(idToken);
        } catch (JwtException e) {
            throw new InvalidGoogleTokenException();
        }

        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        String name = jwt.getClaimAsString("name");

        if (sub == null || email == null || !Boolean.TRUE.equals(emailVerified)) {
            throw new InvalidGoogleTokenException();
        }

        return new GoogleIdentity(sub, email, name != null && !name.isBlank() ? name : email);
    }

    public record GoogleIdentity(String sub, String email, String name) {
    }
}
