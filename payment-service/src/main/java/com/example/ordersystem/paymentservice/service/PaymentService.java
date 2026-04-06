package com.example.ordersystem.paymentservice.service;

import com.example.ordersystem.paymentservice.model.Payment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {
    private static final long PAYMENT_AMOUNT_LIMIT = 999L;
    public PaymentService() {
    }

    public boolean authorize(Payment payment) {
        if(payment.amount().compareTo(BigDecimal.valueOf(PAYMENT_AMOUNT_LIMIT)) > 0){
            return false;
        }
        return true; //todo: make test logic
    }
}
