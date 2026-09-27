package com.detour.trip;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ActivityRepository extends JpaRepository<Activity, UUID> {
    Optional<Activity> findByIdAndTripId(UUID id, UUID tripId);

    @Query("""
            select activity as activity,
                   coalesce(sum(vote.value), 0) as voteScore,
                   coalesce(sum(case when vote.memberId = :memberId then vote.value else 0 end), 0) as currentUserVote
            from Activity activity
            left join ActivityVote vote on vote.activity = activity
            where activity.trip.id = :tripId
            group by activity
            order by activity.date asc, activity.startTime asc
            """)
    List<ActivityWithVotes> findWithVotesByTripId(UUID tripId, UUID memberId);

    interface ActivityWithVotes {
        Activity getActivity();
        long getVoteScore();
        long getCurrentUserVote();
    }
}
