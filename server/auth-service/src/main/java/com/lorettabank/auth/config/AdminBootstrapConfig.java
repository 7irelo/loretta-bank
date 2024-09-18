package com.lorettabank.auth.config;

import com.lorettabank.auth.entity.UserEntity;
import com.lorettabank.auth.repository.UserRepository;
import com.lorettabank.shared.security.JwtConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner bootstrapAdminUser(
            @Value("${auth.bootstrap.enabled:true}") boolean enabled,
            @Value("${auth.bootstrap.admin-email:admin@lorettabank.co.za}") String adminEmail,
            @Value("${auth.bootstrap.admin-password:AdminPass123!}") String adminPassword) {
        return args -> {
            if (!enabled) {
                return;
            }

            if (userRepository.existsByRole(JwtConstants.ROLE_ADMIN)) {
                return;
            }

            if (userRepository.existsByEmail(adminEmail)) {
                log.warn(
                        "Bootstrap admin email {} already exists but has no ADMIN role assigned",
                        adminEmail);
                return;
            }

            UserEntity admin =
                    UserEntity.builder()
                            .email(adminEmail)
                            .passwordHash(passwordEncoder.encode(adminPassword))
                            .firstName("System")
                            .lastName("Admin")
                            .role(JwtConstants.ROLE_ADMIN)
                            .enabled(true)
                            .build();
            userRepository.save(admin);
            log.info("Bootstrapped default admin account: {}", adminEmail);
        };
    }
}
