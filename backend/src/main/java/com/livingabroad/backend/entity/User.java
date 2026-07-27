package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "account_status", nullable = false)
    private String accountStatus = "ACTIVE";

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "auth_provider", nullable = false)
    private String authProvider = "LOCAL";

    @Column(name = "google_sub")
    private String googleSub;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    protected User() {
    }

    public User(String email, String passwordHash, String userName) {
        this(email, passwordHash, userName, "LOCAL", null);
    }

    private User(String email, String passwordHash, String userName, String authProvider, String googleSub) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.userName = userName;
        this.authProvider = authProvider;
        this.googleSub = googleSub;
        this.emailVerified = "GOOGLE".equals(authProvider);
    }

    // Google 로그인으로 처음 가입하는 사용자 — Google이 이미 이메일을 검증했으므로 emailVerified=true로 시작한다.
    public static User forGoogleSignup(String email, String userName, String googleSub) {
        return new User(email, null, userName, "GOOGLE", googleSub);
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getUserName() {
        return userName;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    // 기존 LOCAL 계정의 이메일로 Google 로그인을 시도한 경우 — 같은 이메일을 Google이 검증했으므로
    // 계정을 병합한다. authProvider는 LOCAL로 유지해 기존 비밀번호 로그인도 계속 가능하게 한다.
    public void linkGoogleAccount(String googleSub) {
        this.googleSub = googleSub;
        this.emailVerified = true;
    }
}
