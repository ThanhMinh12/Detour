package com.detour.expense;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/expenses")
public class ExpenseController {
    private final ExpenseService service;

    ExpenseController(ExpenseService service) { this.service = service; }

    @GetMapping
    List<ExpenseResponse> list(@PathVariable UUID tripId) {
        return service.list(tripId).stream().map(this::response).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseResponse create(@PathVariable UUID tripId, @Valid @RequestBody ExpenseRequest request) {
        List<SplitCalculator.Allocation> allocations = request.allocations() == null ? List.of()
                : request.allocations().stream()
                    .map(value -> new SplitCalculator.Allocation(value.memberId(), value.value()))
                    .toList();
        List<SplitCalculator.LineItem> items = request.items() == null ? List.of()
                : request.items().stream()
                    .map(item -> new SplitCalculator.LineItem(item.name(), item.amountCents(), item.participantIds()))
                    .toList();
        return response(service.create(tripId, request.description(), request.paidByMemberId(), request.category(),
                request.splitMode(), request.occurredOn(), request.subtotalCents(), request.taxCents(),
                request.tipCents(), allocations, items));
    }

    private ExpenseResponse response(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getDescription(), expense.getPaidByMemberId(),
                expense.getCategory(), expense.getSplitMode(), expense.getOccurredOn(), expense.getSubtotalCents(),
                expense.getTaxCents(), expense.getTipCents(), expense.getTotalCents(), expense.getCreatedAt(),
                expense.getShares().stream().map(share -> new ShareResponse(share.getMemberId(),
                        share.getSubtotalCents(), share.getTaxCents(), share.getTipCents(), share.getTotalCents())).toList());
    }

    record ExpenseRequest(
            @NotBlank @Size(max = 180) String description,
            @NotNull UUID paidByMemberId,
            @NotNull Expense.Category category,
            @NotNull SplitCalculator.Mode splitMode,
            @NotNull LocalDate occurredOn,
            @Positive long subtotalCents,
            @PositiveOrZero long taxCents,
            @PositiveOrZero long tipCents,
            List<@Valid AllocationRequest> allocations,
            List<@Valid ItemRequest> items) {}

    record AllocationRequest(@NotNull UUID memberId, @PositiveOrZero long value) {}
    record ItemRequest(@NotBlank @Size(max = 160) String name, @Positive long amountCents,
                       @NotEmpty List<@NotNull UUID> participantIds) {}
    record ExpenseResponse(UUID id, String description, UUID paidByMemberId, Expense.Category category,
                           SplitCalculator.Mode splitMode, LocalDate occurredOn, long subtotalCents, long taxCents,
                           long tipCents, long totalCents, Instant createdAt, List<ShareResponse> shares) {}
    record ShareResponse(UUID memberId, long subtotalCents, long taxCents, long tipCents, long totalCents) {}
}

