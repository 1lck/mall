package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.order.application.OrderEventPublisher;
import com.mall.modules.order.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

	/**
	 * 发布订单创建事件，若当前存在事务则延后到事务提交后发送。
	 */
	@Override
	public void publishOrderCreated(OrderCreatedEvent event) {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					doSend(event);
				}
			});
			return;
		}

		doSend(event);
	}

	/**
	 * 把订单事件真正发送到 Kafka。
	 */
	private void doSend(OrderCreatedEvent event) {
		String topic = kafkaTopicsProperties.getTopics().getOrderCreated();
		kafkaTemplate.send(topic, event.orderNo(), event);
		log.info(
			"订单创建事件已发送到消息队列: 消息主题={}, 订单记录编号={}, 订单号={}",
			topic,
			event.orderId(),
			event.orderNo()
		);
	}

}
