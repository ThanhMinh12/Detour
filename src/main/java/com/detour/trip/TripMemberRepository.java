package com.detour.trip;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripMemberRepository extends JpaRepository<TripMember, UUID> {
    List<TripMember> findByTripIdOrderByJoinedAt(UUID tripId);
    Optional<TripMember> findByIdAndTripId(UUID id, UUID tripId);
}

