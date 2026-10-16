package com.detour.trip;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityValidationTest {
    @Test
    void refusesExecutableAndCredentialBearingBookingLinks() {
        for (String value : new String[] {"javascript:alert(1)", "data:text/html,test", "https://user:secret@example.com"}) {
            assertThatThrownBy(() -> new Activity(null, null, LocalDate.now(), null,
                    Activity.Type.LODGING, Activity.Status.CONFIRMED, "Stay", "", "", "", value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
