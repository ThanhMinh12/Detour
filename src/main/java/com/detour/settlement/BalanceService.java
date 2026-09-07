package com.detour.settlement;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.detour.expense.Expense;
import com.detour.expense.ExpenseRepository;
import com.detour.trip.Trip;
import com.detour.trip.TripMember;
import com.detour.trip.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BalanceService {
    private final TripService trips;
    private final ExpenseRepository expenses;
    private final ReimbursementRepository reimbursements;

    BalanceService(TripService trips, ExpenseRepository expenses, ReimbursementRepository reimbursements) {
        this.trips = trips;
        this.expenses = expenses;
        this.reimbursements = reimbursements;
    }

    @Transactional(readOnly = true)
    public List<Balance> balances(UUID tripId) {
        List<TripMember> members = trips.members(tripId);
        Map<UUID, MutableBalance> balances = new LinkedHashMap<>();
        members.forEach(member -> balances.put(member.getId(), new MutableBalance(member.getDisplayName())));

        for (Expense expense : expenses.findByTripIdOrderByOccurredOnDescCreatedAtDesc(tripId)) {
            required(balances, expense.getPaidByMemberId()).amountCents += expense.getTotalCents();
            expense.getShares().forEach(share -> required(balances, share.getMemberId()).amountCents -= share.getTotalCents());
        }
        for (Reimbursement reimbursement : reimbursements.findByTripIdOrderByPaidOnDescCreatedAtDesc(tripId)) {
            required(balances, reimbursement.getFromMemberId()).amountCents += reimbursement.getAmountCents();
            required(balances, reimbursement.getToMemberId()).amountCents -= reimbursement.getAmountCents();
        }

        long sum = balances.values().stream().mapToLong(balance -> balance.amountCents).sum();
        if (sum != 0) throw new IllegalStateException("Ledger is out of balance by " + sum + " minor units");
        return balances.entrySet().stream()
                .map(entry -> new Balance(entry.getKey(), entry.getValue().displayName, entry.getValue().amountCents))
                .toList();
    }

    public Reimbursement reimburse(UUID tripId, UUID fromMemberId, UUID toMemberId, long amountCents,
                                   String note, LocalDate paidOn) {
        Trip trip = trips.get(tripId);
        trips.member(tripId, fromMemberId);
        trips.member(tripId, toMemberId);
        Map<UUID, Long> current = new LinkedHashMap<>();
        balances(tripId).forEach(balance -> current.put(balance.memberId(), balance.amountCents()));
        long fromBalance = current.get(fromMemberId);
        long toBalance = current.get(toMemberId);
        if (fromBalance >= 0 || toBalance <= 0) {
            throw new IllegalArgumentException("A reimbursement must go from a debtor to a creditor");
        }
        if (amountCents > Math.min(Math.abs(fromBalance), toBalance)) {
            throw new IllegalArgumentException("Reimbursement is larger than the current amount owed between balances");
        }
        return reimbursements.save(new Reimbursement(trip, fromMemberId, toMemberId, amountCents, note, paidOn));
    }

    @Transactional(readOnly = true)
    public List<Reimbursement> reimbursements(UUID tripId) {
        trips.get(tripId);
        return reimbursements.findByTripIdOrderByPaidOnDescCreatedAtDesc(tripId);
    }

    private MutableBalance required(Map<UUID, MutableBalance> balances, UUID memberId) {
        MutableBalance balance = balances.get(memberId);
        if (balance == null) throw new IllegalStateException("Ledger references a traveler outside the trip: " + memberId);
        return balance;
    }

    private static class MutableBalance {
        private final String displayName;
        private long amountCents;
        private MutableBalance(String displayName) { this.displayName = displayName; }
    }

    public record Balance(UUID memberId, String displayName, long amountCents) {}
}
