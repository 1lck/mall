package com.mall.modules.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventDispatcherTests {

	@Mock
	private OutboxEventMapper outboxEventRepository;

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void dispatchPendingEventsShouldSendPaymentEventAndMarkSent() {
		OutboxEventDispatcher outboxEventDispatcher =
			new OutboxEventDispatcher(outboxEventRepository, kafkaTemplate, objectMapper);
		OutboxEventEntity event = new OutboxEventEntity();
		event.setEventId("evt-1");
		event.setEventType("PAYMENT_SUCCEEDED");
		event.setTopic("mall.payment.succeeded");
		event.setMessageKey("ORD20260412123456AAAAAA");
		event.setStatus(OutboxEventStatus.PENDING);
		event.setRetryCount(0);
		event.setPayload(objectMapper.valueToTree(new PaymentSucceededEvent(
			"ORD20260412123456AAAAAA",
			new BigDecimal("199.90"),
			Instant.parse("2026-04-12T08:00:00Z")
		)));

		when(outboxEventRepository.findDispatchableBatch(any(), anyInt()))
			.thenReturn(List.of(event));

		outboxEventDispatcher.dispatchPendingEvents();

		ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
		verify(kafkaTemplate).send(
			org.mockito.ArgumentMatchers.eq("mall.payment.succeeded"),
			org.mockito.ArgumentMatchers.eq("ORD20260412123456AAAAAA"),
			payloadCaptor.capture()
		);
		PaymentSucceededEvent sentEvent = (PaymentSucceededEvent) payloadCaptor.getValue();
		assertThat(sentEvent.orderNo()).isEqualTo("ORD20260412123456AAAAAA");
		assertThat(sentEvent.amount()).isEqualByComparingTo("199.90");
		assertThat(sentEvent.paidAt()).isEqualTo(Instant.parse("2026-04-12T08:00:00Z"));

		ArgumentCaptor<Instant> sentAtCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(outboxEventRepository).updateDispatchResult(
			org.mockito.ArgumentMatchers.eq(event.getId()),
			org.mockito.ArgumentMatchers.eq(OutboxEventStatus.SENT),
			org.mockito.ArgumentMatchers.eq(0),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			sentAtCaptor.capture()
		);
		assertThat(sentAtCaptor.getValue()).isNotNull();
	}

	@Test
	void dispatchPendingEventsShouldRecordFailureAndRetryPlanWhenSendFails() {
		OutboxEventDispatcher outboxEventDispatcher =
			new OutboxEventDispatcher(outboxEventRepository, kafkaTemplate, objectMapper);
		OutboxEventEntity event = new OutboxEventEntity();
		event.setEventId("evt-2");
		event.setEventType("PAYMENT_SUCCEEDED");
		event.setTopic("mall.payment.succeeded");
		event.setMessageKey("ORD20260412123456BBBBBB");
		event.setStatus(OutboxEventStatus.PENDING);
		event.setRetryCount(1);
		event.setPayload(objectMapper.valueToTree(new PaymentSucceededEvent(
			"ORD20260412123456BBBBBB",
			new BigDecimal("88.00"),
			Instant.parse("2026-04-12T09:00:00Z")
		)));

		when(outboxEventRepository.findDispatchableBatch(any(), anyInt()))
			.thenReturn(List.of(event));
		when(kafkaTemplate.send(any(), any(), any()))
			.thenThrow(new IllegalStateException("broker unavailable"));

		outboxEventDispatcher.dispatchPendingEvents();

		verify(kafkaTemplate).send(any(), any(), any());

		ArgumentCaptor<Instant> nextRetryAtCaptor = ArgumentCaptor.forClass(Instant.class);
		ArgumentCaptor<String> lastErrorCaptor = ArgumentCaptor.forClass(String.class);
		verify(outboxEventRepository).updateDispatchResult(
			org.mockito.ArgumentMatchers.eq(event.getId()),
			org.mockito.ArgumentMatchers.eq(OutboxEventStatus.FAILED),
			org.mockito.ArgumentMatchers.eq(2),
			nextRetryAtCaptor.capture(),
			lastErrorCaptor.capture(),
			org.mockito.ArgumentMatchers.isNull()
		);
		assertThat(nextRetryAtCaptor.getValue()).isNotNull();
		assertThat(lastErrorCaptor.getValue()).contains("broker unavailable");
	}

	@Test
	void dispatchPendingEventsShouldSkipWhenNoDispatchableEventsExist() {
		OutboxEventDispatcher outboxEventDispatcher =
			new OutboxEventDispatcher(outboxEventRepository, kafkaTemplate, objectMapper);
		when(outboxEventRepository.findDispatchableBatch(any(), anyInt()))
			.thenReturn(List.of());

		outboxEventDispatcher.dispatchPendingEvents();

		verify(kafkaTemplate, never()).send(any(), any(), any());
		verify(outboxEventRepository, never())
			.updateDispatchResult(any(), any(), anyInt(), any(), any(), any());
	}

	@Test
	void dispatchEventByIdShouldSendExactEventWhenItExists() {
		OutboxEventDispatcher outboxEventDispatcher =
			new OutboxEventDispatcher(outboxEventRepository, kafkaTemplate, objectMapper);
		OutboxEventEntity event = new OutboxEventEntity();
		event.setId(7L);
		event.setEventId("evt-7");
		event.setEventType("PAYMENT_SUCCEEDED");
		event.setTopic("mall.payment.succeeded");
		event.setMessageKey("ORD-EXACT-007");
		event.setStatus(OutboxEventStatus.PENDING);
		event.setRetryCount(0);
		event.setPayload(objectMapper.valueToTree(new PaymentSucceededEvent(
			"ORD-EXACT-007",
			new BigDecimal("66.00"),
			Instant.parse("2026-04-12T10:00:00Z")
		)));

		when(outboxEventRepository.findById(7L)).thenReturn(java.util.Optional.of(event));

		outboxEventDispatcher.dispatchEventById(7L);

		verify(kafkaTemplate).send(
			org.mockito.ArgumentMatchers.eq("mall.payment.succeeded"),
			org.mockito.ArgumentMatchers.eq("ORD-EXACT-007"),
			org.mockito.ArgumentMatchers.any()
		);
		verify(outboxEventRepository).updateDispatchResult(
			org.mockito.ArgumentMatchers.eq(7L),
			org.mockito.ArgumentMatchers.eq(OutboxEventStatus.SENT),
			org.mockito.ArgumentMatchers.eq(0),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.any()
		);
	}
}
