package com.detour.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TripWorkflowTest {
    @Autowired TripService trips;

    @Test
    void createsTripWithOrganizerAndAllowsJoiningByInviteCode() {
        Trip trip = trips.create(new Trip("Seoul long weekend", "Seoul", LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 5), "usd"), "Alex", "alex@example.com");

        trips.join(trip.getInviteCode(), "Bo", "bo@example.com");

        assertThat(trips.members(trip.getId())).extracting(TripMember::getDisplayName)
                .containsExactly("Alex", "Bo");
        assertThat(trip.getCurrency()).isEqualTo("USD");
    }
}

