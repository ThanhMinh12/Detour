package com.detour.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import com.detour.expense.Expense;
import com.detour.expense.ExpenseService;
import com.detour.expense.SplitCalculator;
import com.detour.trip.Trip;
import com.detour.trip.TripMember;
import com.detour.trip.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LedgerWorkflowTest {
    @Autowired TripService trips;
    @Autowired ExpenseService expenses;
    @Autowired BalanceService balances;

    @Test
    void derivesNetBalancesFromPayerAndImmutableShares() {
        Trip trip = trips.create(new Trip("Dinner", "Seoul", LocalDate.now(), LocalDate.now(), "USD"),
                "A", "a@example.com");
        TripMember a = trips.members(trip.getId()).getFirst();
        TripMember b = trips.addMember(trip.getId(), "B", "b@example.com");
        TripMember c = trips.addMember(trip.getId(), "C", "c@example.com");
        TripMember d = trips.addMember(trip.getId(), "D", "d@example.com");

        expenses.create(trip.getId(), "Dinner", a.getId(), Expense.Category.FOOD, SplitCalculator.Mode.EXACT,
                LocalDate.now(), 9_000, 900, 1_800, List.of(
                        new SplitCalculator.Allocation(a.getId(), 2_000),
                        new SplitCalculator.Allocation(b.getId(), 3_000),
                        new SplitCalculator.Allocation(c.getId(), 2_500),
                        new SplitCalculator.Allocation(d.getId(), 1_500)), List.of());

        assertThat(balances.balances(trip.getId())).extracting(BalanceService.Balance::amountCents)
                .containsExactly(9_100L, -3_900L, -3_250L, -1_950L);

        balances.reimburse(trip.getId(), b.getId(), a.getId(), 3_900, "Settled dinner share", LocalDate.now());

        assertThat(balances.balances(trip.getId())).extracting(BalanceService.Balance::amountCents)
                .containsExactly(5_200L, 0L, -3_250L, -1_950L);
    }
}
