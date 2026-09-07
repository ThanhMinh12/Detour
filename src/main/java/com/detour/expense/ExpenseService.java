package com.detour.expense;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.detour.trip.Trip;
import com.detour.trip.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseService {
    private final ExpenseRepository expenses;
    private final SplitCalculator calculator;
    private final TripService trips;

    ExpenseService(ExpenseRepository expenses, SplitCalculator calculator, TripService trips) {
        this.expenses = expenses;
        this.calculator = calculator;
        this.trips = trips;
    }

    public Expense create(UUID tripId, String description, UUID paidByMemberId, Expense.Category category,
                          SplitCalculator.Mode mode, LocalDate occurredOn, long subtotalCents, long taxCents,
                          long tipCents, List<SplitCalculator.Allocation> allocations,
                          List<SplitCalculator.LineItem> items) {
        Trip trip = trips.get(tripId);
        trips.member(tripId, paidByMemberId);
        List<SplitCalculator.Share> shares = calculator.calculate(mode, subtotalCents, taxCents, tipCents,
                allocations, items);
        shares.forEach(share -> trips.member(tripId, share.memberId()));
        Expense expense = new Expense(trip, description, paidByMemberId, category, mode, occurredOn,
                subtotalCents, taxCents, tipCents, shares);
        return expenses.save(expense);
    }

    @Transactional(readOnly = true)
    public List<Expense> list(UUID tripId) {
        trips.get(tripId);
        return expenses.findByTripIdOrderByOccurredOnDescCreatedAtDesc(tripId);
    }
}

