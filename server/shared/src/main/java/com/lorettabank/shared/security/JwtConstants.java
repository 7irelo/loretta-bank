package com.lorettabank.shared.security;

public final class JwtConstants {

    private JwtConstants() {}

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SUPPORT = "SUPPORT";
    public static final String ROLE_SERVICE = "SERVICE";
    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    public static final String SYSTEM_USER_ID = "0";
}
