package com.detour.settlement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.detour.trip.Trip;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "reimbursements")
public class Reimbursement {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;
    @Column(nullable = false)
    private UUID fromMemberId;
    @Column(nullable = false)
    private UUID toMemberId;
    @Column(nullable = false)
    private long amountCents;
    @Column(length = 500)
    private String note;
    @Column(nullable = false)
    private LocalDate paidOn;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Reimbursement() {}

    public Reimbursement(Trip trip, UUID fromMemberId, UUID toMemberId, long amountCents, String note, LocalDate paidOn) {
        if (fromMemberId.equals(toMemberId)) throw new IllegalArgumentException("A reimbursement needs two different travelers");
        if (amountCents <= 0) throw new IllegalArgumentException("Reimbursement amount must be greater than zero");
        this.trip = trip;
        this.fromMemberId = fromMemberId;
        this.toMemberId = toMemberId;
        this.amountCents = amountCents;
        this.note = note;
        this.paidOn = paidOn;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getFromMemberId() { return fromMemberId; }
    public UUID getToMemberId() { return toMemberId; }
    public long getAmountCents() { return amountCents; }
    public String getNote() { return note; }
    public LocalDate getPaidOn() { return paidOn; }
    public Instant getCreatedAt() { return createdAt; }
}

