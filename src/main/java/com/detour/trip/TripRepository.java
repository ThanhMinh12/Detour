package com.detour.trip;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TripRepository extends JpaRepository<Trip, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select trip from Trip trip where trip.id = :id")
    Optional<Trip> lockLedger(@org.springframework.data.repository.query.Param("id") UUID id);
    Optional<Trip> findByInviteCode(String inviteCode);
}

