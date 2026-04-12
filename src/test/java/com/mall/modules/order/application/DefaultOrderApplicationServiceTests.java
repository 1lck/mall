package com.mall.modules.order.application;

import com.mall.modules.order.api.CreateOrderRequest;
import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.event.OrderCreatedEvent;
import com.mall.modules.order.persistence.OrderEntity;
import com.mall.modules.order.persistence.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOrderApplicationServiceTests {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderEventPublisher orderEventPublisher;

	@InjectMocks
	private DefaultOrderApplicationService orderApplicationService;

	@Test
	void createOrderShouldPublishOrderCreatedEventAfterPersistingOrder() {
		when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
			OrderEntity order = invocation.getArgument(0);
			setOrderId(order, 101L);
			return order;
		});

		orderApplicationService.createOrder(
			42L,
			new CreateOrderRequest(new BigDecimal("199.90"), "练习订单")
		);

		ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
		verify(orderEventPublisher).publishOrderCreated(eventCaptor.capture());

		OrderCreatedEvent event = eventCaptor.getValue();
		assertThat(event.orderId()).isEqualTo(101L);
		assertThat(event.userId()).isEqualTo(42L);
		assertThat(event.totalAmount()).isEqualByComparingTo("199.90");
		assertThat(event.status()).isEqualTo(OrderStatus.CREATED.name());
		assertThat(event.orderNo()).startsWith("ORD");
	}

	private void setOrderId(OrderEntity order, Long id) {
		try {
			var idField = OrderEntity.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(order, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Failed to set order id for test", exception);
		}
	}
}
