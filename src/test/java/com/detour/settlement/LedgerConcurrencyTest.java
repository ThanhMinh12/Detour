package com.detour.settlement;

import com.detour.auth.AppUserService;
import com.detour.auth.TestAccounts;
import com.detour.expense.Expense;
import com.detour.expense.ExpenseService;
import com.detour.expense.SplitCalculator;
import com.detour.trip.Trip;
import com.detour.trip.TripService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LedgerConcurrencyTest {
    @Autowired TripService trips;
    @Autowired ExpenseService expenses;
    @Autowired BalanceService balances;
    @Autowired AppUserService users;
    @Autowired AuthenticationManager authenticationManager;

    @Test void concurrentRepaymentsCannotBothSpendTheSameDebt() throws Exception {
        try {
            TestAccounts.registerAndSignIn(users, authenticationManager, "Concurrency QA", UUID.randomUUID() + "@example.com");
            var auth = SecurityContextHolder.getContext().getAuthentication();
            var trip = trips.create(new Trip("Concurrent ledger", "Seoul", LocalDate.now(), LocalDate.now(), "USD"));
            var payer = trips.members(trip.getId()).getFirst();
            var debtor = trips.addMember(trip.getId(), "Debtor", "debtor@example.com");
            expenses.create(trip.getId(), "Dinner", payer.getId(), Expense.Category.FOOD,
                    SplitCalculator.Mode.EQUAL, LocalDate.now(), 1000, 0, 0,
                    List.of(new SplitCalculator.Allocation(payer.getId(), 0), new SplitCalculator.Allocation(debtor.getId(), 0)), List.of());
            var gate = new CountDownLatch(1);
            try (var executor = Executors.newFixedThreadPool(2)) {
                java.util.concurrent.Callable<Boolean> pay = () -> {
                    gate.await(); SecurityContextHolder.getContext().setAuthentication(auth);
                    try {
                        balances.reimburse(trip.getId(), debtor.getId(), payer.getId(), 500, "Concurrent QA", LocalDate.now());
                        return true;
                    } catch (IllegalArgumentException expected) { return false; }
                    finally { SecurityContextHolder.clearContext(); }
                };
                var first = executor.submit(pay);
                var second = executor.submit(pay);
                gate.countDown();
                assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            }
            assertThat(balances.balances(trip.getId())).allMatch(balance -> balance.amountCents() == 0);
        } finally { TestAccounts.clear(); }
    }
}
