package com.detour.trip;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripMemberRepository extends JpaRepository<TripMember, UUID> {
    List<TripMember> findByTripIdOrderByJoinedAt(UUID tripId);
    Optional<TripMember> findByIdAndTripId(UUID id, UUID tripId);
    @EntityGraph(attributePaths = "trip")
    Optional<TripMember> findByTripIdAndUserId(UUID tripId, UUID userId);
    Optional<TripMember> findByTripIdAndEmailIgnoreCase(UUID tripId, String email);

    @EntityGraph(attributePaths = "trip")
    @Query("select member from TripMember member where member.userId = :userId order by member.trip.createdAt desc")
    List<TripMember> findTripsForUser(@Param("userId") UUID userId);
}
