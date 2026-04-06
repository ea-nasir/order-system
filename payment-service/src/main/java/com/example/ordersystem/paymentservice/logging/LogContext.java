package com.example.ordersystem.paymentservice.logging;

import org.slf4j.MDC;

public final class LogContext {
    private LogContext() {
    }

    public static void put(String orderId, String eventId, String eventType) {
        if (orderId != null) MDC.put("orderId", orderId);
        if (eventId != null) MDC.put("eventId", eventId);
        if (eventType != null) MDC.put("eventType", eventType);
    }

    public static void clear() {
        MDC.remove("orderId");
        MDC.remove("eventId");
        MDC.remove("eventType");
    }
}