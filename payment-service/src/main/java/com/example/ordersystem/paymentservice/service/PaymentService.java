package com.example.ordersystem.paymentservice.service;

import com.example.ordersystem.paymentservice.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final long PAYMENT_AMOUNT_LIMIT = 999L;

    public boolean authorize(Payment payment) {
        log.info("Authorizing payment: amount={}", payment.amount());

        if (payment.amount().compareTo(BigDecimal.valueOf(PAYMENT_AMOUNT_LIMIT)) > 0) {
            log.warn("Payment authorization declined: amount={}, threshold={}",
                    payment.amount(),
                    PAYMENT_AMOUNT_LIMIT);
            return false;
        }

        log.info("Payment authorization approved: amount={}", payment.amount());
        return true;
    }
}