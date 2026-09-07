package com.detour.expense;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.detour.trip.Trip;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "expenses")
public class Expense {
    public enum Category { FOOD, LODGING, TRANSIT, ACTIVITY, SHOPPING, OTHER }

    @Id @GeneratedValue @UuidGenerator
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private UUID paidByMemberId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Category category;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SplitCalculator.Mode splitMode;
    @Column(nullable = false)
    private LocalDate occurredOn;
    @Column(nullable = false)
    private long subtotalCents;
    @Column(nullable = false)
    private long taxCents;
    @Column(nullable = false)
    private long tipCents;
    @Column(nullable = false)
    private long totalCents;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseShare> shares = new ArrayList<>();

    protected Expense() {}

    public Expense(Trip trip, String description, UUID paidByMemberId, Category category,
                   SplitCalculator.Mode splitMode, LocalDate occurredOn, long subtotalCents,
                   long taxCents, long tipCents, List<SplitCalculator.Share> calculatedShares) {
        this.trip = trip;
        this.description = description.trim();
        this.paidByMemberId = paidByMemberId;
        this.category = category;
        this.splitMode = splitMode;
        this.occurredOn = occurredOn;
        this.subtotalCents = subtotalCents;
        this.taxCents = taxCents;
        this.tipCents = tipCents;
        this.totalCents = Math.addExact(Math.addExact(subtotalCents, taxCents), tipCents);
        this.createdAt = Instant.now();
        calculatedShares.forEach(share -> shares.add(new ExpenseShare(this, share)));
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public String getDescription() { return description; }
    public UUID getPaidByMemberId() { return paidByMemberId; }
    public Category getCategory() { return category; }
    public SplitCalculator.Mode getSplitMode() { return splitMode; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public long getSubtotalCents() { return subtotalCents; }
    public long getTaxCents() { return taxCents; }
    public long getTipCents() { return tipCents; }
    public long getTotalCents() { return totalCents; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ExpenseShare> getShares() { return Collections.unmodifiableList(shares); }
}

