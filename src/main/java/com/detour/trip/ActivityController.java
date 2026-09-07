package com.detour.trip;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/activities")
public class ActivityController {
    private final ActivityService service;
    private final TripService trips;

    ActivityController(ActivityService service, TripService trips) {
        this.service = service;
        this.trips = trips;
    }

    @GetMapping
    List<ActivityResponse> list(@PathVariable UUID tripId) {
        return service.list(tripId).stream().map(this::response).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ActivityResponse create(@PathVariable UUID tripId, @Valid @RequestBody ActivityRequest request) {
        if (request.proposedByMemberId() != null) trips.member(tripId, request.proposedByMemberId());
        Activity activity = new Activity(trips.get(tripId), request.proposedByMemberId(), request.date(), request.startTime(),
                request.type(), request.status(), request.title(), request.place(), request.notes(),
                request.reservationReference(), request.bookingUrl());
        return response(service.save(activity));
    }

    @PutMapping("/{activityId}")
    ActivityResponse update(@PathVariable UUID tripId, @PathVariable UUID activityId,
                            @Valid @RequestBody ActivityRequest request) {
        if (request.proposedByMemberId() != null) trips.member(tripId, request.proposedByMemberId());
        Activity activity = service.get(tripId, activityId);
        activity.update(request.proposedByMemberId(), request.date(), request.startTime(), request.type(), request.status(),
                request.title(), request.place(), request.notes(), request.reservationReference(), request.bookingUrl());
        return response(service.save(activity));
    }

    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID tripId, @PathVariable UUID activityId) { service.delete(tripId, activityId); }

    @PostMapping("/{activityId}/votes")
    VoteResponse vote(@PathVariable UUID tripId, @PathVariable UUID activityId,
                      @Valid @RequestBody VoteRequest request) {
        return new VoteResponse(service.vote(tripId, activityId, request.memberId(), request.value()));
    }

    private ActivityResponse response(Activity activity) {
        return new ActivityResponse(activity.getId(), activity.getProposedByMemberId(), activity.getDate(),
                activity.getStartTime(), activity.getType(), activity.getStatus(), activity.getTitle(), activity.getPlace(),
                activity.getNotes(), activity.getReservationReference(), activity.getBookingUrl(), service.score(activity.getId()));
    }

    record ActivityRequest(UUID proposedByMemberId, @NotNull LocalDate date, LocalTime startTime,
                           @NotNull Activity.Type type, @NotNull Activity.Status status,
                           @NotBlank @Size(max = 160) String title, @Size(max = 240) String place,
                           @Size(max = 2000) String notes, @Size(max = 160) String reservationReference,
                           @Size(max = 500) String bookingUrl) {}
    record VoteRequest(@NotNull UUID memberId, @Min(-1) @Max(1) int value) {}
    record VoteResponse(long score) {}
    record ActivityResponse(UUID id, UUID proposedByMemberId, LocalDate date, LocalTime startTime,
                            Activity.Type type, Activity.Status status, String title, String place, String notes,
                            String reservationReference, String bookingUrl, long voteScore) {}
}

