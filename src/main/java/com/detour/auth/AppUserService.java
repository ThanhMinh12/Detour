package com.detour.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppUserService implements UserDetailsService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    AppUserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser register(String displayName, String email, String password) {
        try {
            return users.saveAndFlush(new AppUser(displayName, email, passwordEncoder.encode(password)));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("An account with that email already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmail(AppUser.normalizeEmail(email))
                .map(AppPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }
}
