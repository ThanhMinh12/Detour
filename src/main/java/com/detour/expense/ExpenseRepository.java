package com.detour.expense;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    @EntityGraph(attributePaths = "shares")
    List<Expense> findByTripIdOrderByOccurredOnDescCreatedAtDesc(UUID tripId);

    @EntityGraph(attributePaths = "shares")
    Optional<Expense> findOneById(UUID id);
}

