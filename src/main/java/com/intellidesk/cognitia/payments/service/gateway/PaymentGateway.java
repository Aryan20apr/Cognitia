package com.intellidesk.cognitia.payments.service.gateway;

import com.intellidesk.cognitia.payments.models.dtos.OrderCreationDTO;
import com.intellidesk.cognitia.payments.models.dtos.OrderDTO;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.PaymentVerificationDTO;

public interface PaymentGateway {

    

    public OrderDTO createOrder(OrderCreationDTO orderCreationDTO);
    public Boolean verifyPayment(PaymentVerificationDTO paymentVerificationDTO);
    
}
