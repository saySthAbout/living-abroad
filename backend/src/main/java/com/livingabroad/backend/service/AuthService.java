package com.livingabroad.backend.service;

import com.livingabroad.backend.dto.auth.AuthResponse;
import com.livingabroad.backend.dto.auth.LoginRequest;
import com.livingabroad.backend.dto.auth.SignupRequest;
import com.livingabroad.backend.entity.User;
import com.livingabroad.backend.exception.EmailAlreadyExistsException;
import com.livingabroad.backend.exception.InvalidCredentialsException;
import com.livingabroad.backend.repository.UserRepository;
import com.livingabroad.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(
            token,
            "Bearer",
            jwtService.expirationSeconds(),
            new AuthResponse.UserSummary(user.getUserId(), user.getUserName(), user.getEmail())
        );
    }
}
