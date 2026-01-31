package com.intellidesk.cognitia.payments.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.intellidesk.cognitia.payments.models.entities.PaymentOrder;
import com.intellidesk.cognitia.payments.models.enums.PaymentVerification;

public interface OrderRepository extends JpaRepository<PaymentOrder, UUID> {

    @Query("SELECT p.orderId FROM PaymentOrder p WHERE p.orderRef = :orderRef")
    Optional<String> findOrderIdByOrderRef(@Param("orderRef") String orderRef);

    @Modifying
    @Query("UPDATE PaymentOrder p SET p.verification = :verification WHERE p.orderRef = :orderRef")
    int updateVerificationByOrderRef(@Param("orderRef") String orderRef, @Param("verification") PaymentVerification verification);
}
