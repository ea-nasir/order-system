package config;

public final class KafkaTopics {
    public static final String ORDERS_CREATED = "orders.created";
    public static final String ORDERS_REJECTED = "orders.rejected";
    public static final String ORDERS_CONFIRMED = "orders.confirmed";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_REJECTED = "inventory.rejected";
    public static final String PAYMENTS_AUTHORIZED = "payments.authorized";
    public static final String PAYMENTS_FAILED = "payments.failed";

    private KafkaTopics() {}
}