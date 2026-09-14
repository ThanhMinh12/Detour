package com.detour.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.detour.auth.AppUserService;
import com.detour.auth.TestAccounts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TripWorkflowTest {
    @Autowired TripService trips;
    @Autowired AppUserService users;
    @Autowired AuthenticationManager authenticationManager;

    @AfterEach
    void clearAuthentication() { TestAccounts.clear(); }

    @Test
    void createsTripWithOrganizerAndAllowsJoiningByInviteCode() {
        TestAccounts.registerAndSignIn(users, authenticationManager, "Alex", "alex@example.com");
        Trip trip = trips.create(new Trip("Seoul long weekend", "Seoul", LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 5), "usd"));

        TestAccounts.registerAndSignIn(users, authenticationManager, "Bo", "bo@example.com");
        trips.join(trip.getInviteCode());

        assertThat(trips.members(trip.getId())).extracting(TripMember::getDisplayName)
                .containsExactly("Alex", "Bo");
        assertThat(trip.getCurrency()).isEqualTo("USD");
    }
}
