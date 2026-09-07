package com.detour.expense;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SplitCalculator {
    public enum Mode { EQUAL, EXACT, PERCENTAGE, ITEMIZED }

    public List<Share> calculate(Mode mode, long subtotalCents, long taxCents, long tipCents,
                                 List<Allocation> allocations, List<LineItem> items) {
        requireNonNegative("subtotalCents", subtotalCents);
        requireNonNegative("taxCents", taxCents);
        requireNonNegative("tipCents", tipCents);
        if (subtotalCents == 0) throw new IllegalArgumentException("subtotalCents must be greater than zero");

        List<Allocation> base = switch (mode) {
            case EQUAL -> equalSubtotal(subtotalCents, allocations);
            case EXACT -> exactSubtotal(subtotalCents, allocations);
            case PERCENTAGE -> percentageSubtotal(subtotalCents, allocations);
            case ITEMIZED -> itemizedSubtotal(subtotalCents, items);
        };

        List<Long> overheadWeights = mode == Mode.PERCENTAGE
                ? allocations.stream().map(Allocation::value).toList()
                : base.stream().map(Allocation::value).toList();
        List<Long> taxes = allocate(taxCents, overheadWeights);
        List<Long> tips = allocate(tipCents, overheadWeights);

        List<Share> result = new ArrayList<>();
        for (int i = 0; i < base.size(); i++) {
            result.add(new Share(base.get(i).memberId(), base.get(i).value(), taxes.get(i), tips.get(i)));
        }
        assertConserved(result, subtotalCents + taxCents + tipCents);
        return List.copyOf(result);
    }

    private List<Allocation> equalSubtotal(long subtotal, List<Allocation> participants) {
        validateUniqueParticipants(participants);
        List<Long> shares = allocate(subtotal, participants.stream().map(ignored -> 1L).toList());
        List<Allocation> result = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            result.add(new Allocation(participants.get(i).memberId(), shares.get(i)));
        }
        return result;
    }

    private List<Allocation> exactSubtotal(long subtotal, List<Allocation> allocations) {
        validateUniqueParticipants(allocations);
        allocations.forEach(value -> requireNonNegative("exact allocation", value.value()));
        if (allocations.stream().mapToLong(Allocation::value).sum() != subtotal) {
            throw new IllegalArgumentException("Exact allocations must add up to subtotalCents");
        }
        return List.copyOf(allocations);
    }

    private List<Allocation> percentageSubtotal(long subtotal, List<Allocation> allocations) {
        validateUniqueParticipants(allocations);
        allocations.forEach(value -> requireNonNegative("percentage basis points", value.value()));
        if (allocations.stream().mapToLong(Allocation::value).sum() != 10_000) {
            throw new IllegalArgumentException("Percentage allocations must add up to 10000 basis points");
        }
        List<Long> amounts = allocate(subtotal, allocations.stream().map(Allocation::value).toList());
        List<Allocation> result = new ArrayList<>();
        for (int i = 0; i < allocations.size(); i++) {
            result.add(new Allocation(allocations.get(i).memberId(), amounts.get(i)));
        }
        return result;
    }

    private List<Allocation> itemizedSubtotal(long subtotal, List<LineItem> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Itemized splits require at least one item");
        if (items.stream().mapToLong(LineItem::amountCents).sum() != subtotal) {
            throw new IllegalArgumentException("Line items must add up to subtotalCents");
        }
        Map<UUID, Long> amounts = new LinkedHashMap<>();
        for (LineItem item : items) {
            if (item.amountCents() <= 0) throw new IllegalArgumentException("Line item amounts must be greater than zero");
            if (item.participantIds() == null || item.participantIds().isEmpty()) {
                throw new IllegalArgumentException("Every line item needs at least one participant");
            }
            if (new HashSet<>(item.participantIds()).size() != item.participantIds().size()) {
                throw new IllegalArgumentException("A traveler cannot appear twice on one line item");
            }
            List<Long> itemShares = allocate(item.amountCents(), item.participantIds().stream().map(ignored -> 1L).toList());
            for (int i = 0; i < item.participantIds().size(); i++) {
                amounts.merge(item.participantIds().get(i), itemShares.get(i), Long::sum);
            }
        }
        return amounts.entrySet().stream().map(entry -> new Allocation(entry.getKey(), entry.getValue())).toList();
    }

    private void validateUniqueParticipants(List<Allocation> allocations) {
        if (allocations == null || allocations.isEmpty()) throw new IllegalArgumentException("At least one participant is required");
        if (allocations.stream().anyMatch(allocation -> allocation.memberId() == null)) {
            throw new IllegalArgumentException("Every allocation needs a memberId");
        }
        Set<UUID> ids = new HashSet<>();
        if (allocations.stream().anyMatch(allocation -> !ids.add(allocation.memberId()))) {
            throw new IllegalArgumentException("Each traveler may appear only once in allocations");
        }
    }

    private List<Long> allocate(long amount, List<Long> weights) {
        if (weights.isEmpty()) throw new IllegalArgumentException("Cannot allocate without weights");
        if (weights.stream().anyMatch(weight -> weight < 0)) throw new IllegalArgumentException("Weights cannot be negative");
        BigInteger totalWeight = weights.stream().map(BigInteger::valueOf).reduce(BigInteger.ZERO, BigInteger::add);
        if (totalWeight.signum() == 0) throw new IllegalArgumentException("Allocation weights cannot all be zero");

        List<Remainder> remainders = new ArrayList<>();
        List<Long> result = new ArrayList<>();
        long assigned = 0;
        for (int i = 0; i < weights.size(); i++) {
            BigInteger[] quotientAndRemainder = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(weights.get(i)))
                    .divideAndRemainder(totalWeight);
            long floor = quotientAndRemainder[0].longValueExact();
            result.add(floor);
            assigned += floor;
            remainders.add(new Remainder(i, quotientAndRemainder[1]));
        }
        remainders.sort(Comparator.comparing(Remainder::remainder).reversed().thenComparing(Remainder::index));
        for (long i = 0; i < amount - assigned; i++) {
            int index = remainders.get((int) i).index();
            result.set(index, result.get(index) + 1);
        }
        return result;
    }

    private void assertConserved(List<Share> shares, long total) {
        if (shares.stream().mapToLong(Share::totalCents).sum() != total) {
            throw new IllegalStateException("Split calculation did not conserve the receipt total");
        }
    }

    private void requireNonNegative(String field, long value) {
        if (value < 0) throw new IllegalArgumentException(field + " cannot be negative");
    }

    private record Remainder(int index, BigInteger remainder) {}
    public record Allocation(UUID memberId, long value) {}
    public record LineItem(String name, long amountCents, List<UUID> participantIds) {}
    public record Share(UUID memberId, long subtotalCents, long taxCents, long tipCents) {
        public long totalCents() { return Math.addExact(Math.addExact(subtotalCents, taxCents), tipCents); }
    }
}

