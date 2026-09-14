package com.detour.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TestAccounts {
    private static final String PASSWORD = "correct-horse-battery-staple";

    private TestAccounts() {}

    public static void registerAndSignIn(AppUserService users, AuthenticationManager manager,
                                         String displayName, String email) {
        users.register(displayName, email, PASSWORD);
        Authentication authentication = manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, PASSWORD));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
