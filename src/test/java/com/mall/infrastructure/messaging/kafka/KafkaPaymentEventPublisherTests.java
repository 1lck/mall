package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaPaymentEventPublisherTests {

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Test
	void publishPaymentSucceededShouldSendEventToConfiguredTopic() {
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
}
