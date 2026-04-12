package com.mall.infrastructure.messaging.kafka;

import com.mall.modules.order.event.OrderCreatedEvent;
import com.mall.modules.order.persistence.OrderEventRecordRepository;
import com.mall.modules.payment.domain.PaymentStatus;
import com.mall.modules.payment.persistence.PaymentRecordEntity;
import com.mall.modules.payment.persistence.PaymentRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatedLoggingConsumerTests {

	@Mock
	private OrderEventRecordRepository orderEventRecordRepository;

	@Mock
	private PaymentRecordRepository paymentRecordRepository;

	@Mock
	private Acknowledgment acknowledgment;

	@InjectMocks
	private OrderCreatedLoggingConsumer orderCreatedLoggingConsumer;

	@Test
	void onOrderCreatedShouldSkipSavingWhenEventAlreadyProcessed() {
		OrderCreatedEvent event = new OrderCreatedEvent(
			101L,
			"ORD20260412123456AAAAAA",
			42L,
			new BigDecimal("199.90"),
			"CREATED",
			Instant.parse("2026-04-12T06:00:00Z")
		);

		when(orderEventRecordRepository.existsByEventTypeAndOrderNo("ORDER_CREATED", event.orderNo()))
			.thenReturn(true);

		orderCreatedLoggingConsumer.onOrderCreated(event, acknowledgment);

		verify(orderEventRecordRepository, never()).save(any());
		verify(paymentRecordRepository, never()).save(any());
		verify(acknowledgment).acknowledge();
	}

	@Test
	void onOrderCreatedShouldCreatePendingPaymentRecordWhenEventFirstProcessed() {
		OrderCreatedEvent event = new OrderCreatedEvent(
			101L,
			"ORD20260412123456AAAAAA",
			42L,
			new BigDecimal("199.90"),
			"CREATED",
			Instant.parse("2026-04-12T06:00:00Z")
		);

		when(orderEventRecordRepository.existsByEventTypeAndOrderNo("ORDER_CREATED", event.orderNo()))
			.thenReturn(false);

		orderCreatedLoggingConsumer.onOrderCreated(event, acknowledgment);

		ArgumentCaptor<PaymentRecordEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentRecordEntity.class);
		verify(paymentRecordRepository).save(paymentCaptor.capture());

		PaymentRecordEntity paymentRecord = paymentCaptor.getValue();
		assertThat(paymentRecord.getOrderNo()).isEqualTo(event.orderNo());
		assertThat(paymentRecord.getAmount()).isEqualByComparingTo("199.90");
		assertThat(paymentRecord.getStatus()).isEqualTo(PaymentStatus.PENDING);
		verify(acknowledgment).acknowledge();
	}
}
