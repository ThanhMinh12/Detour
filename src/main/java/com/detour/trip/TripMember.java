package com.detour.trip;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "trip_members", uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "email"}))
public class TripMember {
    public enum Role { ORGANIZER, MEMBER }

    @Id @GeneratedValue @UuidGenerator
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;
    @Column(nullable = false)
    private String displayName;
    @Column(nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected TripMember() {}

    public TripMember(Trip trip, String displayName, String email, Role role) {
        this.trip = trip;
        this.displayName = displayName.trim();
        this.email = email.trim().toLowerCase();
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}

