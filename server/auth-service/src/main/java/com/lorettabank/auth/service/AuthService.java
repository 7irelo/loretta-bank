package com.lorettabank.auth.service;

import com.lorettabank.auth.dto.AuthResponse;
import com.lorettabank.auth.dto.LoginRequest;
import com.lorettabank.auth.dto.RefreshRequest;
import com.lorettabank.auth.dto.RegisterRequest;
import com.lorettabank.auth.dto.UpdateRoleRequest;
import com.lorettabank.auth.dto.UserResponse;
import com.lorettabank.auth.entity.UserEntity;
import com.lorettabank.auth.repository.UserRepository;
import com.lorettabank.shared.exception.DuplicateResourceException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import com.lorettabank.shared.exception.UnauthorizedException;
import com.lorettabank.shared.security.JwtConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "User with email " + request.getEmail() + " already exists");
        }

        UserEntity user =
                UserEntity.builder()
                        .email(request.getEmail())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .role(JwtConstants.ROLE_CUSTOMER)
                        .enabled(true)
                        .build();

        user = userRepository.save(user);
        log.info("User registered successfully with email: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(
                                () -> new UnauthorizedException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        log.info("User logged in successfully: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.validateToken(request.getRefreshToken());
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new UnauthorizedException("Token is not a refresh token");
        }

        String jti = jwtService.extractJti(claims);
        if (isTokenBlacklisted(jti)) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        Long userId = Long.parseLong(jwtService.extractUserId(claims));
        UserEntity user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new UnauthorizedException("User not found"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        log.info("Token refreshed for user: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public void logout(String accessTokenJti, String refreshToken, long accessTokenExpMs) {
        // Blacklist the access token
        redisTemplate
                .opsForValue()
                .set(
                        BLACKLIST_PREFIX + accessTokenJti,
                        "blacklisted",
                        accessTokenExpMs,
                        TimeUnit.MILLISECONDS);

        // Blacklist the refresh token
        try {
            Claims refreshClaims = jwtService.validateToken(refreshToken);
            String refreshJti = jwtService.extractJti(refreshClaims);
            long refreshExpMs =
                    refreshClaims.getExpiration().getTime() - System.currentTimeMillis();
            if (refreshExpMs > 0) {
                redisTemplate
                        .opsForValue()
                        .set(
                                BLACKLIST_PREFIX + refreshJti,
                                "blacklisted",
                                refreshExpMs,
                                TimeUnit.MILLISECONDS);
            }
        } catch (JwtException e) {
            log.warn("Could not blacklist refresh token: {}", e.getMessage());
        }

        log.info("Tokens blacklisted successfully");
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        UserEntity user =
                userRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with id: " + id));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserRole(Long id, UpdateRoleRequest request) {
        String normalizedRole = request.getRole().toUpperCase();
        if (!List.of(
                        JwtConstants.ROLE_ADMIN,
                        JwtConstants.ROLE_CUSTOMER,
                        JwtConstants.ROLE_SUPPORT)
                .contains(normalizedRole)) {
            throw new IllegalArgumentException("Invalid role. Allowed: ADMIN, CUSTOMER, SUPPORT");
        }

        UserEntity user =
                userRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with id: " + id));

        user.setRole(normalizedRole);
        UserEntity saved = userRepository.save(user);
        log.info("Updated role for user {} to {}", saved.getEmail(), normalizedRole);
        return toUserResponse(saved);
    }

    private boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private UserResponse toUserResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
