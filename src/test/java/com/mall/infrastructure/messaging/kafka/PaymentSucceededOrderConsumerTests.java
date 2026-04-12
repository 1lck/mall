package com.mall.infrastructure.messaging.kafka;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.persistence.OrderEntity;
import com.mall.modules.order.persistence.OrderEventRecordRepository;
import com.mall.modules.order.persistence.OrderRepository;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSucceededOrderConsumerTests {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderEventRecordRepository orderEventRecordRepository;

	@Mock
	private Acknowledgment acknowledgment;

	@InjectMocks
	private PaymentSucceededOrderConsumer paymentSucceededOrderConsumer;

	@Test
	void onPaymentSucceededShouldUpdateOrderToPaidWhenFirstProcessed() {
		PaymentSucceededEvent event = new PaymentSucceededEvent(
			"ORD20260412123456AAAAAA",
			new BigDecimal("199.90"),
			Instant.parse("2026-04-12T08:00:00Z")
		);
		OrderEntity order = new OrderEntity();
		order.setOrderNo(event.orderNo());
		order.setUserId(42L);
		order.setTotalAmount(event.amount());
		order.setStatus(OrderStatus.CREATED);

		when(orderEventRecordRepository.claimProcessing("PAYMENT_SUCCEEDED", event.orderNo()))
			.thenReturn(1);
		when(orderRepository.findByOrderNo(event.orderNo()))
			.thenReturn(Optional.of(order));

		paymentSucceededOrderConsumer.onPaymentSucceeded(event, acknowledgment);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		verify(orderRepository).save(order);
		verify(orderEventRecordRepository, never()).save(any());
		verify(acknowledgment).acknowledge();
	}

	@Test
	void onPaymentSucceededShouldSkipWhenEventAlreadyProcessed() {
		PaymentSucceededEvent event = new PaymentSucceededEvent(
			"ORD20260412123456AAAAAA",
			new BigDecimal("199.90"),
			Instant.parse("2026-04-12T08:00:00Z")
		);

		when(orderEventRecordRepository.claimProcessing("PAYMENT_SUCCEEDED", event.orderNo()))
			.thenReturn(0);

		paymentSucceededOrderConsumer.onPaymentSucceeded(event, acknowledgment);

		verify(orderRepository, never()).save(any());
		verify(orderEventRecordRepository, never()).save(any());
		verify(acknowledgment).acknowledge();
	}

	@Test
	void onPaymentSucceededShouldAcknowledgeAndStopWhenBusinessExceptionOccurs() {
		PaymentSucceededEvent event = new PaymentSucceededEvent(
			"ORD404",
			new BigDecimal("199.90"),
			Instant.parse("2026-04-12T08:00:00Z")
		);

		when(orderEventRecordRepository.claimProcessing("PAYMENT_SUCCEEDED", event.orderNo()))
			.thenReturn(1);
		when(orderRepository.findByOrderNo(event.orderNo()))
			.thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Order ORD404 was not found"));

		assertThatCode(() -> paymentSucceededOrderConsumer.onPaymentSucceeded(event, acknowledgment))
			.doesNotThrowAnyException();

		verify(acknowledgment).acknowledge();
		verify(orderRepository, never()).save(any());
	}

	@Test
	void onPaymentSucceededShouldRethrowSystemExceptionForRetry() {
		PaymentSucceededEvent event = new PaymentSucceededEvent(
			"ORD20260412123456AAAAAA",
			new BigDecimal("199.90"),
			Instant.parse("2026-04-12T08:00:00Z")
		);

		when(orderEventRecordRepository.claimProcessing("PAYMENT_SUCCEEDED", event.orderNo()))
			.thenReturn(1);
		when(orderRepository.findByOrderNo(event.orderNo()))
			.thenThrow(new IllegalStateException("database temporarily unavailable"));

		assertThatThrownBy(() -> paymentSucceededOrderConsumer.onPaymentSucceeded(event, acknowledgment))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("temporarily unavailable");

		verify(acknowledgment, never()).acknowledge();
		verify(orderRepository, never()).save(any());
	}
}
