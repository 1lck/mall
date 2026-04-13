package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.order.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaOrderEventPublisherTests {

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Test
	void publishOrderCreatedShouldSendEventToConfiguredTopicWithoutTransaction() {
		KafkaTopicsProperties properties = new KafkaTopicsProperties();
		KafkaTopicsProperties.Topics topics = new KafkaTopicsProperties.Topics();
		topics.setOrderCreated("mall.order.created");
		properties.setTopics(topics);

		KafkaOrderEventPublisher publisher = new KafkaOrderEventPublisher(kafkaTemplate, properties);
		OrderCreatedEvent event = new OrderCreatedEvent(
			101L,
			"ORD20260412123456AAAAAA",
			42L,
			new BigDecimal("199.90"),
			"CREATED",
			Instant.parse("2026-04-12T06:00:00Z")
		);

		publisher.publishOrderCreated(event);

		verify(kafkaTemplate).send("mall.order.created", event.orderNo(), event);
	}

	@Test
	void publishOrderCreatedShouldDeferSendUntilAfterCommitWhenTransactionActive() {
		KafkaTopicsProperties properties = new KafkaTopicsProperties();
		KafkaTopicsProperties.Topics topics = new KafkaTopicsProperties.Topics();
		topics.setOrderCreated("mall.order.created");
		properties.setTopics(topics);

		KafkaOrderEventPublisher publisher = new KafkaOrderEventPublisher(kafkaTemplate, properties);
		OrderCreatedEvent event = new OrderCreatedEvent(
			101L,
			"ORD20260412123456AAAAAA",
			42L,
			new BigDecimal("199.90"),
			"CREATED",
			Instant.parse("2026-04-12T06:00:00Z")
		);

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		try {
			publisher.publishOrderCreated(event);

			verify(kafkaTemplate, never()).send("mall.order.created", event.orderNo(), event);

			List<TransactionSynchronization> synchronizations =
				TransactionSynchronizationManager.getSynchronizations();
			for (TransactionSynchronization synchronization : synchronizations) {
				synchronization.afterCommit();
			}

			verify(kafkaTemplate).send("mall.order.created", event.orderNo(), event);
		} finally {
			TransactionSynchronizationManager.setActualTransactionActive(false);
			TransactionSynchronizationManager.clearSynchronization();
		}
	}
}
