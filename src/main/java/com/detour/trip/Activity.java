package com.detour.trip;

import java.time.LocalDate;
import java.time.LocalTime;
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
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "activities")
public class Activity {
    public enum Type { ACTIVITY, FOOD, LODGING, TRANSIT }
    public enum Status { PROPOSED, CONFIRMED, CANCELLED }

    @Id @GeneratedValue @UuidGenerator
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;
    private UUID proposedByMemberId;
    @Column(nullable = false)
    private LocalDate date;
    private LocalTime startTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Type type;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status;
    @Column(nullable = false)
    private String title;
    private String place;
    @Column(length = 2000)
    private String notes;
    private String reservationReference;
    private String bookingUrl;

    protected Activity() {}

    public Activity(Trip trip, UUID proposedByMemberId, LocalDate date, LocalTime startTime, Type type,
                    Status status, String title, String place, String notes, String reservationReference,
                    String bookingUrl) {
        this.trip = trip;
        update(proposedByMemberId, date, startTime, type, status, title, place, notes, reservationReference, bookingUrl);
    }

    public void update(UUID proposedByMemberId, LocalDate date, LocalTime startTime, Type type, Status status,
                       String title, String place, String notes, String reservationReference, String bookingUrl) {
        this.proposedByMemberId = proposedByMemberId;
        this.date = date;
        this.startTime = startTime;
        this.type = type;
        this.status = status;
        this.title = title.trim();
        this.place = place;
        this.notes = notes;
        this.reservationReference = reservationReference;
        this.bookingUrl = bookingUrl;
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public UUID getProposedByMemberId() { return proposedByMemberId; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public Type getType() { return type; }
    public Status getStatus() { return status; }
    public String getTitle() { return title; }
    public String getPlace() { return place; }
    public String getNotes() { return notes; }
    public String getReservationReference() { return reservationReference; }
    public String getBookingUrl() { return bookingUrl; }
}

