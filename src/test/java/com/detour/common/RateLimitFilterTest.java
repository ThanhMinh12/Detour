package com.detour.common;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {
    @Test void rejectsExcessRequestsAndRecoversAfterTheWindow() {
        AtomicLong clock = new AtomicLong();
        var limiter = new RateLimitFilter.WindowLimiter(1000, 2, clock::get);
        assertThat(limiter.allow("one", 2)).isTrue();
        assertThat(limiter.allow("one", 2)).isTrue();
        assertThat(limiter.allow("one", 2)).isFalse();
        clock.set(1000);
        assertThat(limiter.allow("one", 2)).isTrue();
    }
    @Test void boundsMemoryEvenWhenClientKeysChange() {
        var limiter = new RateLimitFilter.WindowLimiter(1000, 2, () -> 0);
        assertThat(limiter.allow("one", 2)).isTrue();
        assertThat(limiter.allow("two", 2)).isTrue();
        assertThat(limiter.allow("three", 2)).isFalse();
    }
}
