package com.lorettabank.auth.controller;

import com.lorettabank.auth.dto.AuthResponse;
import com.lorettabank.auth.dto.LoginRequest;
import com.lorettabank.auth.dto.RefreshRequest;
import com.lorettabank.auth.dto.RegisterRequest;
import com.lorettabank.auth.dto.UpdateRoleRequest;
import com.lorettabank.auth.dto.UserResponse;
import com.lorettabank.auth.service.AuthService;
import com.lorettabank.auth.service.JwtService;
import com.lorettabank.shared.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody RefreshRequest request) {
        String accessToken = extractTokenFromHeader(authorizationHeader);
        Claims claims = jwtService.validateToken(accessToken);
        String jti = jwtService.extractJti(claims);
        long expMs = claims.getExpiration().getTime() - System.currentTimeMillis();

        authService.logout(jti, request.getRefreshToken(), Math.max(expMs, 0));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = authService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = authService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        UserResponse user = authService.updateUserRole(id, request);
        return ResponseEntity.ok(user);
    }

    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        return authorizationHeader.substring(7);
    }
}
