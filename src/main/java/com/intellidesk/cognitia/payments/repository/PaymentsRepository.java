package com.intellidesk.cognitia.payments.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.payments.models.entities.Payment;

public interface PaymentsRepository extends JpaRepository<Payment, UUID> {
    
}
