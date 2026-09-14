package com.detour.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserService users;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessions = new ChangeSessionIdAuthenticationStrategy();

    AuthController(AppUserService users, AuthenticationManager authenticationManager,
                   SecurityContextRepository contexts) {
        this.users = users;
        this.authenticationManager = authenticationManager;
        this.contexts = contexts;
    }

    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody RegisterRequest request,
                          HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        users.register(request.displayName(), request.email(), request.password());
        return authenticate(request.email(), request.password(), servletRequest, servletResponse);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request,
                       HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        return authenticate(request.email(), request.password(), servletRequest, servletResponse);
    }

    @GetMapping("/me")
    AuthResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal principal)) {
            return new AuthResponse(false, null);
        }
        return response(principal);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    private AuthResponse authenticate(String email, String password, HttpServletRequest request,
                                      HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(AppUser.normalizeEmail(email), password));
        sessions.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return response((AppPrincipal) authentication.getPrincipal());
    }

    private AuthResponse response(AppPrincipal principal) {
        return new AuthResponse(true, new UserResponse(principal.id(), principal.displayName(), principal.email()));
    }

    record RegisterRequest(@NotBlank @Size(max = 100) String displayName,
                           @NotBlank @Email @Size(max = 320) String email,
                           @NotBlank @Size(min = 8, max = 72) String password) {}
    record LoginRequest(@NotBlank @Email @Size(max = 320) String email,
                        @NotBlank @Size(min = 8, max = 72) String password) {}
    record CsrfResponse(String headerName, String token) {}
    record UserResponse(java.util.UUID id, String displayName, String email) {}
    record AuthResponse(boolean authenticated, UserResponse user) {}
}
