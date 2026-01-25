package com.intellidesk.cognitia.payments.service.gateway;

import com.intellidesk.cognitia.payments.models.dtos.OrderCreationDTO;
import com.intellidesk.cognitia.payments.models.dtos.OrderDTO;

public interface PaymentGateway {

    

    public OrderDTO createOrder(OrderCreationDTO orderCreationDTO);
    
}
