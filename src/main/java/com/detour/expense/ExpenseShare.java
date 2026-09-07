package com.detour.expense;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "expense_shares", uniqueConstraints = @UniqueConstraint(columnNames = {"expense_id", "member_id"}))
public class ExpenseShare {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;
    @Column(name = "member_id", nullable = false)
    private UUID memberId;
    @Column(nullable = false)
    private long subtotalCents;
    @Column(nullable = false)
    private long taxCents;
    @Column(nullable = false)
    private long tipCents;
    @Column(nullable = false)
    private long totalCents;

    protected ExpenseShare() {}

    ExpenseShare(Expense expense, SplitCalculator.Share share) {
        this.expense = expense;
        this.memberId = share.memberId();
        this.subtotalCents = share.subtotalCents();
        this.taxCents = share.taxCents();
        this.tipCents = share.tipCents();
        this.totalCents = share.totalCents();
    }

    public UUID getMemberId() { return memberId; }
    public long getSubtotalCents() { return subtotalCents; }
    public long getTaxCents() { return taxCents; }
    public long getTipCents() { return tipCents; }
    public long getTotalCents() { return totalCents; }
}

