package com.intellidesk.cognitia.payments.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.payments.models.entities.PaymentOrder;

public interface OrderRepository extends JpaRepository<PaymentOrder, UUID> {
    
}
