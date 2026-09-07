package com.detour.trip;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TripRepository extends JpaRepository<Trip, UUID> {
    Optional<Trip> findByInviteCode(String inviteCode);
}

