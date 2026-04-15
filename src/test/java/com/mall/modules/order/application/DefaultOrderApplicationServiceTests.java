package com.mall.modules.order.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.order.dto.CreateOrderDTO;
import com.mall.modules.order.dto.UpdateOrderDTO;
import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.event.OrderCreatedEvent;
import com.mall.modules.order.persistence.entity.OrderEntity;
import com.mall.modules.order.persistence.mapper.OrderMapper;
import com.mall.modules.product.domain.ProductStatus;
import com.mall.modules.product.persistence.entity.ProductEntity;
import com.mall.modules.product.persistence.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOrderApplicationServiceTests {

	@Mock
	private OrderMapper orderRepository;

	@Mock
	private OrderEventPublisher orderEventPublisher;

	@Mock
	private ProductMapper productRepository;

	@InjectMocks
	private DefaultOrderApplicationService orderApplicationService;

	@Test
	void createOrderShouldPublishOrderCreatedEventAfterPersistingOrderAndDeductStock() {
		ProductEntity product = buildProduct(8, new BigDecimal("99.95"));
		when(productRepository.findById(7L)).thenReturn(Optional.of(product));
		when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
			OrderEntity order = invocation.getArgument(0);
			setOrderId(order, 101L);
			return order;
		});

		orderApplicationService.createOrder(
			42L,
			new CreateOrderDTO(7L, 2, "练习订单")
		);

		ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
		verify(orderEventPublisher).publishOrderCreated(eventCaptor.capture());

		OrderCreatedEvent event = eventCaptor.getValue();
		assertThat(event.orderId()).isEqualTo(101L);
		assertThat(event.userId()).isEqualTo(42L);
		assertThat(event.totalAmount()).isEqualByComparingTo("199.90");
		assertThat(event.status()).isEqualTo(OrderStatus.CREATED.name());
		assertThat(event.orderNo()).startsWith("ORD");
		assertThat(product.getStock()).isEqualTo(6);
		verify(productRepository).save(product);
	}

	@Test
	void createOrderShouldRejectWhenProductStockIsInsufficient() {
		ProductEntity product = buildProduct(1, new BigDecimal("99.95"));
		when(productRepository.findById(7L)).thenReturn(Optional.of(product));

		assertThatThrownBy(() -> orderApplicationService.createOrder(
			42L,
			new CreateOrderDTO(7L, 2, "库存不足订单")
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.BAD_REQUEST);
	}

	@Test
	void updateOrderShouldRejectNonAdminUser() {
		OrderEntity order = buildOrder(OrderStatus.CREATED, new BigDecimal("199.90"));
		when(orderRepository.findByIdAndUserId(9L, 42L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderApplicationService.updateOrder(
			42L,
			false,
			9L,
			new UpdateOrderDTO(new BigDecimal("299.50"), OrderStatus.CANCELLED, "普通用户尝试修改")
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	void updateOrderShouldRejectManualPaidTransitionEvenForAdmin() {
		OrderEntity order = buildOrder(OrderStatus.CREATED, new BigDecimal("199.90"));
		when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderApplicationService.updateOrder(
			42L,
			true,
			9L,
			new UpdateOrderDTO(new BigDecimal("299.50"), OrderStatus.PAID, "管理员尝试手动改成已支付")
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.BAD_REQUEST);
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

	private ProductEntity buildProduct(int stock, BigDecimal price) {
		ProductEntity product = new ProductEntity();
		product.setProductNo("PRD202604130001");
		product.setName("练习商品");
		product.setCategoryName("练习分类");
		product.setPrice(price.setScale(2, RoundingMode.HALF_UP));
		product.setStock(stock);
		product.setStatus(ProductStatus.ON_SALE);
		product.setDescription("练习商品描述");
		return product;
	}

	private OrderEntity buildOrder(OrderStatus status, BigDecimal totalAmount) {
		OrderEntity order = new OrderEntity();
		setOrderId(order, 9L);
		order.setOrderNo("ORD202604130009AAAAAA");
		order.setUserId(42L);
		order.setProductId(7L);
		order.setQuantity(2);
		order.setTotalAmount(totalAmount);
		order.setStatus(status);
		order.setRemark("原始订单");
		return order;
	}
}
