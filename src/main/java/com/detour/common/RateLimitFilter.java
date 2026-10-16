package com.detour.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded single-instance throttling; use a shared edge limiter when scaling out. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class RateLimitFilter extends OncePerRequestFilter {
    private final WindowLimiter limiter = new WindowLimiter(60_000, 4096, System::currentTimeMillis);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/") && !"GET".equals(request.getMethod()) && !"OPTIONS".equals(request.getMethod())) {
            String category = path.startsWith("/api/auth/") ? "auth" : path.startsWith("/api/trips/join/") ? "join" : "write";
            int maximum = "auth".equals(category) ? 20 : "join".equals(category) ? 30 : 120;
            if (!limiter.allow("global", 600) || !limiter.allow(request.getRemoteAddr() + ":" + category, maximum)) {
                response.setStatus(429);
                response.setHeader("Retry-After", "60");
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Too many requests. Please wait a minute and try again.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    static final class WindowLimiter {
        private record Window(long start, int count) {}
        private final Map<String, Window> windows = new HashMap<>();
        private final long duration;
        private final int capacity;
        private final LongSupplier clock;
        WindowLimiter(long duration, int capacity, LongSupplier clock) {
            this.duration = duration; this.capacity = capacity; this.clock = clock;
        }
        synchronized boolean allow(String key, int maximum) {
            long now = clock.getAsLong();
            Window window = windows.get(key);
            if (window == null || now - window.start() >= duration) {
                if (windows.size() >= capacity) windows.entrySet().removeIf(entry -> now - entry.getValue().start() >= duration);
                if (!windows.containsKey(key) && windows.size() >= capacity) return false;
                window = new Window(now, 0);
            }
            if (window.count() >= maximum) return false;
            windows.put(key, new Window(window.start(), window.count() + 1));
            return true;
        }
    }
}
