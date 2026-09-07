package com.detour.trip;

import java.util.List;
import java.util.UUID;

import com.detour.common.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TripService {
    private final TripRepository trips;
    private final TripMemberRepository members;

    TripService(TripRepository trips, TripMemberRepository members) {
        this.trips = trips;
        this.members = members;
    }

    public Trip create(Trip trip, String organizerName, String organizerEmail) {
        Trip saved = trips.save(trip);
        members.save(new TripMember(saved, organizerName, organizerEmail, TripMember.Role.ORGANIZER));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Trip> list() { return trips.findAll(); }

    @Transactional(readOnly = true)
    public Trip get(UUID id) {
        return trips.findById(id).orElseThrow(() -> new NotFoundException("Trip " + id + " was not found"));
    }

    public TripMember addMember(UUID tripId, String displayName, String email) {
        try {
            return members.saveAndFlush(new TripMember(get(tripId), displayName, email, TripMember.Role.MEMBER));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("That email is already a member of this trip");
        }
    }

    public TripMember join(String inviteCode, String displayName, String email) {
        Trip trip = trips.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Invite code is not valid"));
        return addMember(trip.getId(), displayName, email);
    }

    @Transactional(readOnly = true)
    public List<TripMember> members(UUID tripId) {
        get(tripId);
        return members.findByTripIdOrderByJoinedAt(tripId);
    }

    @Transactional(readOnly = true)
    public TripMember member(UUID tripId, UUID memberId) {
        return members.findByIdAndTripId(memberId, tripId)
                .orElseThrow(() -> new NotFoundException("Traveler " + memberId + " is not in this trip"));
    }
}

