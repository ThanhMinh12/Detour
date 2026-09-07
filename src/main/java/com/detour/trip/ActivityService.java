package com.detour.trip;

import java.util.List;
import java.util.UUID;

import com.detour.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActivityService {
    private final ActivityRepository activities;
    private final ActivityVoteRepository votes;
    private final TripService trips;

    ActivityService(ActivityRepository activities, ActivityVoteRepository votes, TripService trips) {
        this.activities = activities;
        this.votes = votes;
        this.trips = trips;
    }

    public Activity save(Activity activity) { return activities.save(activity); }

    @Transactional(readOnly = true)
    public List<Activity> list(UUID tripId) {
        trips.get(tripId);
        return activities.findByTripIdOrderByDateAscStartTimeAsc(tripId);
    }

    @Transactional(readOnly = true)
    public Activity get(UUID tripId, UUID activityId) {
        return activities.findByIdAndTripId(activityId, tripId)
                .orElseThrow(() -> new NotFoundException("Activity " + activityId + " was not found in this trip"));
    }

    public void delete(UUID tripId, UUID activityId) { activities.delete(get(tripId, activityId)); }

    public long vote(UUID tripId, UUID activityId, UUID memberId, int value) {
        Activity activity = get(tripId, activityId);
        trips.member(tripId, memberId);
        ActivityVote vote = votes.findByActivityIdAndMemberId(activityId, memberId)
                .orElseGet(() -> new ActivityVote(activity, memberId, value));
        vote.setValue(value);
        votes.save(vote);
        return votes.score(activityId);
    }

    @Transactional(readOnly = true)
    public long score(UUID activityId) { return votes.score(activityId); }
}

