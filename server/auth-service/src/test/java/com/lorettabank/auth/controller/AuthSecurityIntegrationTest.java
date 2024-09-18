package com.lorettabank.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lorettabank.auth.config.AuthSecurityConfig;
import com.lorettabank.auth.dto.AuthResponse;
import com.lorettabank.auth.service.AuthService;
import com.lorettabank.auth.service.JwtService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(AuthSecurityConfig.class)
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    void registerShouldBeAccessibleWithoutAuthentication() throws Exception {
        when(authService.register(any()))
                .thenReturn(
                        AuthResponse.builder()
                                .accessToken("access")
                                .refreshToken("refresh")
                                .userId(1L)
                                .email("john@lorettabank.co.za")
                                .role("CUSTOMER")
                                .build());

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email":"john@lorettabank.co.za",
                                          "password":"SecurePass123!",
                                          "firstName":"John",
                                          "lastName":"Doe"
                                        }
                                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void usersEndpointShouldRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/auth/users")).andExpect(status().is4xxClientError());
    }

    @Test
    void usersEndpointShouldAllowAdminRoleFromGatewayHeaders() throws Exception {
        when(authService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(
                        get("/api/v1/auth/users")
                                .header("X-User-Id", "99")
                                .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }
}
