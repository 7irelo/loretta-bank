package com.lorettabank.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.util.List;
import org.junit.jupiter.api.Test;

class InternalTokenServiceTest {

    private final InternalTokenService tokens =
            new InternalTokenService("test-internal-secret-that-is-long-enough-0123456789");

    @Test
    void roundTripsSubjectAndRoles() {
        var claims = tokens.verify(tokens.mint("42", List.of("CUSTOMER"), "a@b.co"));
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("roles", List.class)).containsExactly("CUSTOMER");
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String forged =
                new InternalTokenService("another-secret-that-is-also-long-enough-000000")
                        .mint("1", List.of("ADMIN"), null);
        assertThatThrownBy(() -> tokens.verify(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> tokens.verify("not-a-token")).isInstanceOf(JwtException.class);
    }

    @Test
    void refusesShortSecrets() {
        assertThatThrownBy(() -> new InternalTokenService("short"))
                .isInstanceOf(IllegalStateException.class);
    }
}
