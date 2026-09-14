package com.detour.trip;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.detour.auth.AppPrincipal;
import com.detour.auth.CurrentUser;
import com.detour.common.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TripService {
    private final TripRepository trips;
    private final TripMemberRepository members;
    private final CurrentUser currentUser;

    TripService(TripRepository trips, TripMemberRepository members, CurrentUser currentUser) {
        this.trips = trips;
        this.members = members;
        this.currentUser = currentUser;
    }

    public Trip create(Trip trip) {
        AppPrincipal user = currentUser.required();
        Trip saved = trips.save(trip);
        members.save(new TripMember(saved, user.displayName(), user.email(), TripMember.Role.ORGANIZER, user.id()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Trip> list() {
        return members.findTripsForUser(currentUser.required().id()).stream().map(TripMember::getTrip).toList();
    }

    @Transactional(readOnly = true)
    public Trip get(UUID id) {
        return members.findByTripIdAndUserId(id, currentUser.required().id())
                .map(TripMember::getTrip)
                .orElseThrow(() -> new NotFoundException("Trip " + id + " was not found"));
    }

    public TripMember addMember(UUID tripId, String displayName, String email) {
        try {
            return members.saveAndFlush(new TripMember(get(tripId), displayName, email, TripMember.Role.MEMBER));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("That email is already a member of this trip");
        }
    }

    public TripMember join(String inviteCode) {
        AppPrincipal user = currentUser.required();
        Trip trip = trips.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Invite code is not valid"));
        Optional<TripMember> existingAccount = members.findByTripIdAndUserId(trip.getId(), user.id());
        if (existingAccount.isPresent()) return existingAccount.get();

        Optional<TripMember> invitedTraveler = members.findByTripIdAndEmailIgnoreCase(trip.getId(), user.email());
        if (invitedTraveler.isPresent()) {
            TripMember member = invitedTraveler.get();
            member.attachUser(user.id(), user.displayName(), user.email());
            return members.save(member);
        }

        try {
            return members.saveAndFlush(new TripMember(trip, user.displayName(), user.email(),
                    TripMember.Role.MEMBER, user.id()));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("You are already a member of this trip");
        }
    }

    @Transactional(readOnly = true)
    public List<TripMember> members(UUID tripId) {
        get(tripId);
        return members.findByTripIdOrderByJoinedAt(tripId);
    }

    @Transactional(readOnly = true)
    public TripMember member(UUID tripId, UUID memberId) {
        get(tripId);
        return members.findByIdAndTripId(memberId, tripId)
                .orElseThrow(() -> new NotFoundException("Traveler " + memberId + " is not in this trip"));
    }

    @Transactional(readOnly = true)
    public TripMember currentMember(UUID tripId) {
        return members.findByTripIdAndUserId(tripId, currentUser.required().id())
                .orElseThrow(() -> new NotFoundException("Trip " + tripId + " was not found"));
    }
}
