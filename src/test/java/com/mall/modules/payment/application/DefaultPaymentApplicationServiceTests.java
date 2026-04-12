package com.mall.modules.payment.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import com.mall.modules.payment.domain.PaymentStatus;
import com.mall.modules.payment.persistence.PaymentRecordEntity;
import com.mall.modules.payment.persistence.PaymentRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
	private PaymentEventPublisher paymentEventPublisher;

	@InjectMocks
	private DefaultPaymentApplicationService paymentApplicationService;

	@Test
	void markPaymentSuccessShouldUpdatePaymentStatusToSuccess() {
		PaymentRecordEntity paymentRecord = new PaymentRecordEntity();
		paymentRecord.setOrderNo("ORD20260412123456AAAAAA");
		paymentRecord.setAmount(new BigDecimal("199.90"));
		paymentRecord.setStatus(PaymentStatus.PENDING);

		when(paymentRecordRepository.findByOrderNo("ORD20260412123456AAAAAA"))
			.thenReturn(Optional.of(paymentRecord));

		paymentApplicationService.markPaymentSuccess("ORD20260412123456AAAAAA");

		assertThat(paymentRecord.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
		verify(paymentRecordRepository).save(paymentRecord);

		ArgumentCaptor<PaymentSucceededEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSucceededEvent.class);
		verify(paymentEventPublisher).publishPaymentSucceeded(eventCaptor.capture());
		assertThat(eventCaptor.getValue().orderNo()).isEqualTo("ORD20260412123456AAAAAA");
		assertThat(eventCaptor.getValue().amount()).isEqualByComparingTo("199.90");
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

		when(paymentRecordRepository.findByOrderNo("ORD20260412123456BBBBBB"))
			.thenReturn(Optional.of(paymentRecord));

		assertThatThrownBy(() -> paymentApplicationService.markPaymentSuccess("ORD20260412123456BBBBBB"))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
			});

		verify(paymentRecordRepository).findByOrderNo("ORD20260412123456BBBBBB");
	}
}
