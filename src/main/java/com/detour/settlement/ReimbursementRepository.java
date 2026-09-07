package com.detour.settlement;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReimbursementRepository extends JpaRepository<Reimbursement, UUID> {
    List<Reimbursement> findByTripIdOrderByPaidOnDescCreatedAtDesc(UUID tripId);
}

