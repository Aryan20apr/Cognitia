package com.intellidesk.cognitia.payments.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.intellidesk.cognitia.payments.models.entities.PaymentOrder;
import com.intellidesk.cognitia.payments.models.enums.FulfillmentStatus;
import com.intellidesk.cognitia.payments.models.enums.PaymentVerification;

public interface OrderRepository extends JpaRepository<PaymentOrder, UUID> {

    @Query("SELECT p.orderId FROM PaymentOrder p WHERE p.orderRef = :orderRef")
    Optional<String> findOrderIdByOrderRef(@Param("orderRef") String orderRef);

    @Modifying
    @Query("UPDATE PaymentOrder p SET p.verification = :verification WHERE p.orderRef = :orderRef")
    int updateVerificationByOrderRef(@Param("orderRef") String orderRef, @Param("verification") PaymentVerification verification);

    Optional<PaymentOrder> findByOrderRef(String orderRef);
    
    @Modifying
    @Query("UPDATE PaymentOrder p SET p.verification = :verification WHERE p.orderId = :orderId")
    int updateVerificationByOrderId(@Param("orderId") String orderId, @Param("verification") PaymentVerification verification);
    
    @Modifying
    @Query("UPDATE PaymentOrder p SET p.fulfillmentStatus = :status WHERE p.orderId = :orderId")
    int updateFulfillmentStatusByOrderId(@Param("orderId") String orderId, @Param("status") FulfillmentStatus status);
    
    Optional<PaymentOrder> findByOrderId(String orderId);
}
