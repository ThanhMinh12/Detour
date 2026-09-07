package com.detour.settlement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SettlementEngine {
    public static final int OPTIMAL_GROUP_LIMIT = 12;
    public enum Strategy { GREEDY, OPTIMAL }

    public Settlement settle(List<BalanceService.Balance> allBalances, Strategy requestedStrategy) {
        List<BalanceService.Balance> balances = allBalances.stream()
                .filter(balance -> balance.amountCents() != 0)
                .toList();
        long sum = balances.stream().mapToLong(BalanceService.Balance::amountCents).sum();
        if (sum != 0) throw new IllegalArgumentException("Balances must sum to zero");

        if (requestedStrategy == Strategy.OPTIMAL && balances.size() <= OPTIMAL_GROUP_LIMIT) {
            return new Settlement(Strategy.OPTIMAL, optimal(balances), false);
        }
        boolean fellBack = requestedStrategy == Strategy.OPTIMAL;
        return new Settlement(Strategy.GREEDY, greedy(balances), fellBack);
    }

    private List<Transfer> greedy(List<BalanceService.Balance> balances) {
        List<Position> creditors = balances.stream()
                .filter(balance -> balance.amountCents() > 0)
                .map(Position::new)
                .sorted(Comparator.comparingLong(Position::amountCents).reversed().thenComparing(Position::memberId))
                .toList();
        List<Position> debtors = balances.stream()
                .filter(balance -> balance.amountCents() < 0)
                .map(Position::new)
                .sorted(Comparator.comparingLong((Position position) -> position.amountCents())
                        .thenComparing(Position::memberId))
                .toList();
        long[] credit = creditors.stream().mapToLong(Position::amountCents).toArray();
        long[] debt = debtors.stream().mapToLong(position -> -position.amountCents()).toArray();
        List<Transfer> result = new ArrayList<>();
        int creditor = 0;
        int debtor = 0;
        while (creditor < credit.length && debtor < debt.length) {
            long amount = Math.min(credit[creditor], debt[debtor]);
            result.add(transfer(debtors.get(debtor), creditors.get(creditor), amount));
            credit[creditor] -= amount;
            debt[debtor] -= amount;
            if (credit[creditor] == 0) creditor++;
            if (debt[debtor] == 0) debtor++;
        }
        return List.copyOf(result);
    }

    private List<Transfer> optimal(List<BalanceService.Balance> balances) {
        if (balances.isEmpty()) return List.of();
        List<Position> positions = balances.stream().map(Position::new).toList();
        long[] amounts = positions.stream().mapToLong(Position::amountCents).toArray();
        Best best = new Best(greedy(balances));
        search(positions, amounts, new ArrayList<>(), best);
        return List.copyOf(best.transfers);
    }

    private void search(List<Position> positions, long[] amounts, List<Transfer> current, Best best) {
        if (current.size() >= best.transfers.size()) return;
        int first = 0;
        while (first < amounts.length && amounts[first] == 0) first++;
        if (first == amounts.length) {
            best.transfers = List.copyOf(current);
            return;
        }

        Set<Long> triedCounterpartBalances = new HashSet<>();
        long firstAmount = amounts[first];
        for (int counterpart = first + 1; counterpart < amounts.length; counterpart++) {
            long counterpartAmount = amounts[counterpart];
            if (counterpartAmount == 0 || Long.signum(firstAmount) == Long.signum(counterpartAmount)
                    || !triedCounterpartBalances.add(counterpartAmount)) continue;

            long transferAmount = Math.min(Math.abs(firstAmount), Math.abs(counterpartAmount));
            Position from = firstAmount < 0 ? positions.get(first) : positions.get(counterpart);
            Position to = firstAmount < 0 ? positions.get(counterpart) : positions.get(first);
            long oldFirst = amounts[first];
            long oldCounterpart = amounts[counterpart];
            amounts[first] += firstAmount < 0 ? transferAmount : -transferAmount;
            amounts[counterpart] += counterpartAmount < 0 ? transferAmount : -transferAmount;
            current.add(transfer(from, to, transferAmount));
            search(positions, amounts, current, best);
            current.remove(current.size() - 1);
            amounts[first] = oldFirst;
            amounts[counterpart] = oldCounterpart;

            if (oldFirst + oldCounterpart == 0) break;
        }
    }

    private Transfer transfer(Position from, Position to, long amount) {
        return new Transfer(from.memberId(), from.displayName(), to.memberId(), to.displayName(), amount);
    }

    private record Position(UUID memberId, String displayName, long amountCents) {
        Position(BalanceService.Balance balance) {
            this(balance.memberId(), balance.displayName(), balance.amountCents());
        }
    }

    private static class Best {
        private List<Transfer> transfers;
        private Best(List<Transfer> transfers) { this.transfers = List.copyOf(transfers); }
    }

    public record Transfer(UUID fromMemberId, String fromName, UUID toMemberId, String toName, long amountCents) {}
    public record Settlement(Strategy strategyUsed, List<Transfer> transfers, boolean fellBackFromOptimal) {}
}
