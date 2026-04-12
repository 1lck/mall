package com.mall.modules.payment.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.persistence.OrderEntity;
import com.mall.modules.order.persistence.OrderRepository;
import com.mall.modules.payment.domain.PaymentStatus;
import com.mall.modules.payment.persistence.PaymentRecordEntity;
import com.mall.modules.payment.persistence.PaymentRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPaymentApplicationServiceTests {

	@Mock
	private PaymentRecordRepository paymentRecordRepository;

	@Mock
	private OrderRepository orderRepository;

	@InjectMocks
	private DefaultPaymentApplicationService paymentApplicationService;

	@Test
	void markPaymentSuccessShouldUpdatePaymentStatusToSuccess() {
		PaymentRecordEntity paymentRecord = new PaymentRecordEntity();
		paymentRecord.setOrderNo("ORD20260412123456AAAAAA");
		paymentRecord.setAmount(new BigDecimal("199.90"));
		paymentRecord.setStatus(PaymentStatus.PENDING);
		OrderEntity order = new OrderEntity();
		order.setOrderNo("ORD20260412123456AAAAAA");
		order.setUserId(42L);
		order.setTotalAmount(new BigDecimal("199.90"));
		order.setStatus(OrderStatus.CREATED);

		when(paymentRecordRepository.findByOrderNo("ORD20260412123456AAAAAA"))
			.thenReturn(Optional.of(paymentRecord));
		when(orderRepository.findByOrderNo("ORD20260412123456AAAAAA"))
			.thenReturn(Optional.of(order));

		paymentApplicationService.markPaymentSuccess("ORD20260412123456AAAAAA");

		assertThat(paymentRecord.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		verify(paymentRecordRepository).save(paymentRecord);
		verify(orderRepository).save(order);
	}

	@Test
	void markPaymentSuccessShouldThrowWhenPaymentRecordDoesNotExist() {
		when(paymentRecordRepository.findByOrderNo("ORD404"))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentApplicationService.markPaymentSuccess("ORD404"))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
			});
	}

	@Test
	void markPaymentSuccessShouldRejectInvalidStatusTransition() {
		PaymentRecordEntity paymentRecord = new PaymentRecordEntity();
		paymentRecord.setOrderNo("ORD20260412123456BBBBBB");
		paymentRecord.setAmount(new BigDecimal("199.90"));
		paymentRecord.setStatus(PaymentStatus.FAILED);
		OrderEntity order = new OrderEntity();
		order.setOrderNo("ORD20260412123456BBBBBB");
		order.setUserId(42L);
		order.setTotalAmount(new BigDecimal("199.90"));
		order.setStatus(OrderStatus.CREATED);

		when(paymentRecordRepository.findByOrderNo("ORD20260412123456BBBBBB"))
			.thenReturn(Optional.of(paymentRecord));
		when(orderRepository.findByOrderNo("ORD20260412123456BBBBBB"))
			.thenReturn(Optional.of(order));

		assertThatThrownBy(() -> paymentApplicationService.markPaymentSuccess("ORD20260412123456BBBBBB"))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
			});

		verify(paymentRecordRepository).findByOrderNo("ORD20260412123456BBBBBB");
	}
}
