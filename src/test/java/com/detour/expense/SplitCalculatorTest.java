package com.detour.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SplitCalculatorTest {
    private final SplitCalculator calculator = new SplitCalculator();
    private final UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID b = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private final UUID c = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private final UUID d = UUID.fromString("00000000-0000-0000-0000-00000000000d");

    @Test
    void allocatesTaxAndTipProportionallyToEachDiner() {
        List<SplitCalculator.Share> shares = calculator.calculate(SplitCalculator.Mode.EXACT, 9_000, 900, 1_800,
                List.of(allocation(a, 2_000), allocation(b, 3_000), allocation(c, 2_500), allocation(d, 1_500)),
                List.of());

        assertThat(shares).extracting(SplitCalculator.Share::totalCents)
                .containsExactly(2_600L, 3_900L, 3_250L, 1_950L);
        assertThat(shares).extracting(SplitCalculator.Share::taxCents)
                .containsExactly(200L, 300L, 250L, 150L);
        assertThat(shares).extracting(SplitCalculator.Share::tipCents)
                .containsExactly(400L, 600L, 500L, 300L);
    }

    @Test
    void sharesItemsAndConservesEveryRoundingCent() {
        List<SplitCalculator.Share> shares = calculator.calculate(SplitCalculator.Mode.ITEMIZED, 2_001, 173, 401,
                List.of(), List.of(
                        new SplitCalculator.LineItem("Shared noodles", 1_001, List.of(a, b, c)),
                        new SplitCalculator.LineItem("Tea", 1_000, List.of(b, c))));

        assertThat(shares).extracting(SplitCalculator.Share::subtotalCents)
                .containsExactly(334L, 834L, 833L);
        assertThat(shares.stream().mapToLong(SplitCalculator.Share::totalCents).sum()).isEqualTo(2_575L);
    }

    @Test
    void percentageSplitUsesBasisPointsAndRejectsIncompletePercentages() {
        assertThatThrownBy(() -> calculator.calculate(SplitCalculator.Mode.PERCENTAGE, 10_000, 0, 0,
                List.of(allocation(a, 6_000), allocation(b, 3_999)), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10000 basis points");
    }

    private SplitCalculator.Allocation allocation(UUID memberId, long value) {
        return new SplitCalculator.Allocation(memberId, value);
    }
}
