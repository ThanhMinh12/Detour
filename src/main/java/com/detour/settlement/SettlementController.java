package com.detour.settlement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.detour.trip.TripService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}")
public class SettlementController {
    private final BalanceService balances;
    private final SettlementEngine settlements;
    private final TripService trips;

    SettlementController(BalanceService balances, SettlementEngine settlements, TripService trips) {
        this.balances = balances;
        this.settlements = settlements;
        this.trips = trips;
    }

    @GetMapping("/balances")
    BalanceResponse balances(@PathVariable UUID tripId) {
        return new BalanceResponse(trips.get(tripId).getCurrency(), balances.balances(tripId));
    }

    @GetMapping("/settlements")
    SettlementResponse settlement(@PathVariable UUID tripId,
                                  @RequestParam(defaultValue = "OPTIMAL") SettlementEngine.Strategy strategy) {
        SettlementEngine.Settlement result = settlements.settle(balances.balances(tripId), strategy);
        return new SettlementResponse(trips.get(tripId).getCurrency(), result.strategyUsed(),
                result.fellBackFromOptimal(), result.transfers());
    }

    @GetMapping("/reimbursements")
    List<ReimbursementResponse> reimbursements(@PathVariable UUID tripId) {
        return balances.reimbursements(tripId).stream().map(this::response).toList();
    }

    @PostMapping("/reimbursements")
    @ResponseStatus(HttpStatus.CREATED)
    ReimbursementResponse reimburse(@PathVariable UUID tripId, @Valid @RequestBody ReimbursementRequest request) {
        return response(balances.reimburse(tripId, request.fromMemberId(), request.toMemberId(), request.amountCents(),
                request.note(), request.paidOn()));
    }

    private ReimbursementResponse response(Reimbursement reimbursement) {
        return new ReimbursementResponse(reimbursement.getId(), reimbursement.getFromMemberId(),
                reimbursement.getToMemberId(), reimbursement.getAmountCents(), reimbursement.getNote(),
                reimbursement.getPaidOn(), reimbursement.getCreatedAt());
    }

    record ReimbursementRequest(@NotNull UUID fromMemberId, @NotNull UUID toMemberId,
                                @Positive long amountCents, @Size(max = 500) String note,
                                @NotNull LocalDate paidOn) {}
    record ReimbursementResponse(UUID id, UUID fromMemberId, UUID toMemberId, long amountCents,
                                 String note, LocalDate paidOn, Instant createdAt) {}
    record BalanceResponse(String currency, List<BalanceService.Balance> balances) {}
    record SettlementResponse(String currency, SettlementEngine.Strategy strategyUsed,
                              boolean fellBackFromOptimal, List<SettlementEngine.Transfer> transfers) {}
}

