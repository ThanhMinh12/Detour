package com.detour.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SettlementEngineTest {
    private final SettlementEngine engine = new SettlementEngine();

    @Test
    void exactSearchCanBeatLargestFirstGreedyMatching() {
        List<BalanceService.Balance> balances = List.of(
                balance("A", 10), balance("B", 9), balance("C", 5),
                balance("D", -14), balance("E", -10));

        SettlementEngine.Settlement greedy = engine.settle(balances, SettlementEngine.Strategy.GREEDY);
        SettlementEngine.Settlement optimal = engine.settle(balances, SettlementEngine.Strategy.OPTIMAL);

        assertThat(greedy.transfers()).hasSize(4);
        assertThat(optimal.transfers()).hasSize(3);
        assertThat(optimal.strategyUsed()).isEqualTo(SettlementEngine.Strategy.OPTIMAL);
        assertConservesBalances(balances, optimal.transfers());
    }

    @Test
    void reproducesAValidThreePaymentSettlementForExampleBalances() {
        List<BalanceService.Balance> balances = List.of(
                balance("A", 12_000), balance("B", -4_000), balance("C", 3_000), balance("D", -11_000));

        SettlementEngine.Settlement result = engine.settle(balances, SettlementEngine.Strategy.OPTIMAL);

        assertThat(result.transfers()).hasSize(3);
        assertConservesBalances(balances, result.transfers());
    }

    private void assertConservesBalances(List<BalanceService.Balance> balances,
                                         List<SettlementEngine.Transfer> transfers) {
        for (BalanceService.Balance balance : balances) {
            long sent = transfers.stream().filter(value -> value.fromMemberId().equals(balance.memberId()))
                    .mapToLong(SettlementEngine.Transfer::amountCents).sum();
            long received = transfers.stream().filter(value -> value.toMemberId().equals(balance.memberId()))
                    .mapToLong(SettlementEngine.Transfer::amountCents).sum();
            assertThat(balance.amountCents() + sent - received).isZero();
        }
    }

    private BalanceService.Balance balance(String name, long cents) {
        return new BalanceService.Balance(UUID.nameUUIDFromBytes(name.getBytes()), name, cents);
    }
}

