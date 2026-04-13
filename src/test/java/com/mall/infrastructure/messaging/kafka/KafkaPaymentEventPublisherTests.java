package com.mall.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import com.mall.modules.outbox.application.OutboxDispatchTrigger;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaPaymentEventPublisherTests {

	@Mock
	private OutboxEventMapper outboxEventRepository;

	@Mock
	private OutboxDispatchTrigger outboxDispatchTrigger;

	@Test
	void publishPaymentSucceededShouldSavePendingOutboxEvent() {
		KafkaTopicsProperties properties = new KafkaTopicsProperties();
		KafkaTopicsProperties.Topics topics = new KafkaTopicsProperties.Topics();
		topics.setPaymentSucceeded("mall.payment.succeeded");
		properties.setTopics(topics);

		KafkaPaymentEventPublisher publisher = new KafkaPaymentEventPublisher(
			properties,
			outboxEventRepository,
			outboxDispatchTrigger,
			new ObjectMapper().findAndRegisterModules()
		);
		PaymentSucceededEvent event = new PaymentSucceededEvent(
			"ORD20260412123456AAAAAA",
			new BigDecimal("199.90"),
			Instant.parse("2026-04-12T08:00:00Z")
		);
		org.mockito.Mockito.doAnswer(invocation -> {
			OutboxEventEntity saved = invocation.getArgument(0);
			saved.setId(101L);
			return saved;
		}).when(outboxEventRepository).save(org.mockito.ArgumentMatchers.any(OutboxEventEntity.class));

		publisher.publishPaymentSucceeded(event);

		ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
		verify(outboxEventRepository).save(captor.capture());
		OutboxEventEntity savedEvent = captor.getValue();
		assertThat(savedEvent.getEventId()).isNotBlank();
		assertThat(savedEvent.getAggregateType()).isEqualTo("PAYMENT");
		assertThat(savedEvent.getAggregateId()).isEqualTo(event.orderNo());
		assertThat(savedEvent.getEventType()).isEqualTo("PAYMENT_SUCCEEDED");
		assertThat(savedEvent.getTopic()).isEqualTo("mall.payment.succeeded");
		assertThat(savedEvent.getMessageKey()).isEqualTo(event.orderNo());
		assertThat(savedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
		assertThat(savedEvent.getRetryCount()).isZero();
		assertThat(savedEvent.getPayload().get("orderNo").asText()).isEqualTo(event.orderNo());
		assertThat(savedEvent.getPayload().get("amount").decimalValue())
			.isEqualByComparingTo("199.90");
		verify(outboxDispatchTrigger).requestDispatch(101L);
	}
}
