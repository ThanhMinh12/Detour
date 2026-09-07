package com.detour.trip;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "trips")
public class Trip {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String destination;
    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false, unique = true, length = 12)
    private String inviteCode;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Trip() {}

    public Trip(String name, String destination, LocalDate startDate, LocalDate endDate, String currency) {
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("endDate cannot be before startDate");
        this.name = name.trim();
        this.destination = destination.trim();
        this.startDate = startDate;
        this.endDate = endDate;
        this.currency = currency.toUpperCase(Locale.ROOT);
        this.inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDestination() { return destination; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getCurrency() { return currency; }
    public String getInviteCode() { return inviteCode; }
    public Instant getCreatedAt() { return createdAt; }
}

