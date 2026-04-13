package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.payment.event.PaymentSucceededEvent;
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
class KafkaPaymentEventPublisherTests {

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Test
	void publishPaymentSucceededShouldSendEventToConfiguredTopicWithoutTransaction() {
		KafkaTopicsProperties properties = new KafkaTopicsProperties();
		KafkaTopicsProperties.Topics topics = new KafkaTopicsProperties.Topics();
		topics.setPaymentSucceeded("mall.payment.succeeded");
		properties.setTopics(topics);

		KafkaPaymentEventPublisher publisher = new KafkaPaymentEventPublisher(kafkaTemplate, properties);
		PaymentSucceededEvent event = new PaymentSucceededEvent(
			"ORD20260412123456AAAAAA",
			new BigDecimal("199.90"),
			Instant.parse("2026-04-12T08:00:00Z")
		);

		publisher.publishPaymentSucceeded(event);

		verify(kafkaTemplate).send("mall.payment.succeeded", event.orderNo(), event);
	}

	@Test
	void publishPaymentSucceededShouldDeferSendUntilAfterCommitWhenTransactionActive() {
		KafkaTopicsProperties properties = new KafkaTopicsProperties();
		KafkaTopicsProperties.Topics topics = new KafkaTopicsProperties.Topics();
		topics.setPaymentSucceeded("mall.payment.succeeded");
		properties.setTopics(topics);

		KafkaPaymentEventPublisher publisher = new KafkaPaymentEventPublisher(kafkaTemplate, properties);
		PaymentSucceededEvent event = new PaymentSucceededEvent(
			"ORD20260412123456AAAAAA",
			new BigDecimal("199.90"),
			Instant.parse("2026-04-12T08:00:00Z")
		);

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		try {
			publisher.publishPaymentSucceeded(event);

			verify(kafkaTemplate, never()).send("mall.payment.succeeded", event.orderNo(), event);

			List<TransactionSynchronization> synchronizations =
				TransactionSynchronizationManager.getSynchronizations();
			for (TransactionSynchronization synchronization : synchronizations) {
				synchronization.afterCommit();
			}

			verify(kafkaTemplate).send("mall.payment.succeeded", event.orderNo(), event);
		} finally {
			TransactionSynchronizationManager.setActualTransactionActive(false);
			TransactionSynchronizationManager.clearSynchronization();
		}
	}
}
