package com.detour.trip;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService service;

    TripController(TripService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TripResponse create(@Valid @RequestBody CreateTripRequest request) {
        Trip trip = new Trip(request.name(), request.destination(), request.startDate(), request.endDate(), request.currency());
        return response(service.create(trip, request.organizerName(), request.organizerEmail()));
    }

    @GetMapping
    List<TripResponse> list() { return service.list().stream().map(this::response).toList(); }

    @GetMapping("/{tripId}")
    TripResponse get(@PathVariable UUID tripId) { return response(service.get(tripId)); }

    @GetMapping("/{tripId}/members")
    List<MemberResponse> members(@PathVariable UUID tripId) {
        return service.members(tripId).stream().map(this::response).toList();
    }

    @PostMapping("/{tripId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    MemberResponse addMember(@PathVariable UUID tripId, @Valid @RequestBody AddMemberRequest request) {
        return response(service.addMember(tripId, request.displayName(), request.email()));
    }

    @PostMapping("/join/{inviteCode}")
    @ResponseStatus(HttpStatus.CREATED)
    MemberResponse join(@PathVariable String inviteCode, @Valid @RequestBody AddMemberRequest request) {
        return response(service.join(inviteCode, request.displayName(), request.email()));
    }

    private TripResponse response(Trip trip) {
        return new TripResponse(trip.getId(), trip.getName(), trip.getDestination(), trip.getStartDate(),
                trip.getEndDate(), trip.getCurrency(), trip.getInviteCode(), trip.getCreatedAt());
    }

    private MemberResponse response(TripMember member) {
        return new MemberResponse(member.getId(), member.getDisplayName(), member.getEmail(), member.getRole(), member.getJoinedAt());
    }

    record CreateTripRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 160) String destination,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotBlank @Size(max = 100) String organizerName,
            @NotBlank @Email String organizerEmail) {}

    record AddMemberRequest(@NotBlank @Size(max = 100) String displayName, @NotBlank @Email String email) {}
    record TripResponse(UUID id, String name, String destination, LocalDate startDate, LocalDate endDate,
                        String currency, String inviteCode, Instant createdAt) {}
    record MemberResponse(UUID id, String displayName, String email, TripMember.Role role, Instant joinedAt) {}
}

