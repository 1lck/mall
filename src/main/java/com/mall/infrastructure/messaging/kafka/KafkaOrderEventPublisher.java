package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.order.application.OrderEventPublisher;
import com.mall.modules.order.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Kafka 的订单事件发布器。
 *
 * <p>它是 OrderEventPublisher 的一个具体实现，
 * 负责把业务层传进来的事件真正发送到 Kafka topic。</p>
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class KafkaOrderEventPublisher implements OrderEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final KafkaTopicsProperties kafkaTopicsProperties;

	public KafkaOrderEventPublisher(
		KafkaTemplate<String, Object> kafkaTemplate,
		KafkaTopicsProperties kafkaTopicsProperties
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.kafkaTopicsProperties = kafkaTopicsProperties;
	}

	@Override
	public void publishOrderCreated(OrderCreatedEvent event) {
		// topic 名称不在代码里写死，统一从配置里拿。
		// 这样你后面改 topic，不用回头改业务代码。
		String topic = kafkaTopicsProperties.getTopics().getOrderCreated();

		// 这里真正把消息发到 Kafka：
		// topic = 发到哪个主题
		// key   = 用订单号做消息 key，后面更方便按订单维度观察消息
		// value = 真正的订单创建事件
		kafkaTemplate.send(topic, event.orderNo(), event);
		log.info(
			"Kafka order event published: topic={}, orderId={}, orderNo={}",
			topic,
			event.orderId(),
			event.orderNo()
		);
	}
}
