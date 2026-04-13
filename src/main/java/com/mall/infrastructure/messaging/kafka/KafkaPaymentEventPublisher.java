package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.payment.application.PaymentEventPublisher;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 基于 Kafka 的支付事件发布器。
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final KafkaTopicsProperties kafkaTopicsProperties;

	public KafkaPaymentEventPublisher(
		KafkaTemplate<String, Object> kafkaTemplate,
		KafkaTopicsProperties kafkaTopicsProperties
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.kafkaTopicsProperties = kafkaTopicsProperties;
	}

	@Override
	public void publishPaymentSucceeded(PaymentSucceededEvent event) {
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

	private void doSend(PaymentSucceededEvent event) {
		String topic = kafkaTopicsProperties.getTopics().getPaymentSucceeded();
		kafkaTemplate.send(topic, event.orderNo(), event);
		log.info("Kafka payment succeeded event published: topic={}, orderNo={}", topic, event.orderNo());
	}
}
