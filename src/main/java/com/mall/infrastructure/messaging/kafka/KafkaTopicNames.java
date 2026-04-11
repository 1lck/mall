package com.mall.infrastructure.messaging.kafka;

/**
 * Kafka topic 常量定义。
 */
public final class KafkaTopicNames {

	public static final String ORDER_CREATED = "mall.order.created";
	public static final String ORDER_CANCELLED = "mall.order.cancelled";
	public static final String INVENTORY_RESERVED = "mall.inventory.reserved";
	public static final String INVENTORY_RELEASED = "mall.inventory.released";

	private KafkaTopicNames() {
	}
}
