package com.example.ordersystem.paymentservice.service;

import com.example.ordersystem.paymentservice.model.Payment;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    public PaymentService() {
    }

    public boolean authorize(Payment payment) {
        return true; //todo: make test logic
    }
}
