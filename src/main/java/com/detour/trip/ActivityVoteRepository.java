package com.detour.trip;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ActivityVoteRepository extends JpaRepository<ActivityVote, UUID> {
    Optional<ActivityVote> findByActivityIdAndMemberId(UUID activityId, UUID memberId);
    @Query("select coalesce(sum(v.value), 0) from ActivityVote v where v.activity.id = :activityId")
    long score(UUID activityId);
}

