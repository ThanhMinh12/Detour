package com.detour.trip;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findByTripIdOrderByDateAscStartTimeAsc(UUID tripId);
    Optional<Activity> findByIdAndTripId(UUID id, UUID tripId);
}

