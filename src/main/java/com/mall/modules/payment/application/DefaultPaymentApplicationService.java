package com.mall.modules.payment.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.persistence.OrderEntity;
import com.mall.modules.order.persistence.OrderRepository;
import com.mall.modules.payment.domain.PaymentStatus;
import com.mall.modules.payment.persistence.PaymentRecordEntity;
import com.mall.modules.payment.persistence.PaymentRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付应用服务实现。
 *
 * <p>当前先只实现最小版“支付成功”流转：
 * 按订单号找到支付记录，然后把状态从 PENDING 改成 SUCCESS。</p>
 */
@Service
@Transactional
public class DefaultPaymentApplicationService implements PaymentApplicationService {

	private final PaymentRecordRepository paymentRecordRepository;
	private final OrderRepository orderRepository;

	public DefaultPaymentApplicationService(
		PaymentRecordRepository paymentRecordRepository,
		OrderRepository orderRepository
	) {
		this.paymentRecordRepository = paymentRecordRepository;
		this.orderRepository = orderRepository;
	}

	@Override
	public void markPaymentSuccess(String orderNo) {
		// 先查支付记录，查不到说明这笔订单还没创建支付任务。
		PaymentRecordEntity paymentRecord = paymentRecordRepository.findByOrderNo(orderNo)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.NOT_FOUND,
				"Payment record for order " + orderNo + " was not found"
			));

		// 再查订单本身，后面支付成功后需要把订单状态一起推进到已支付。
		OrderEntity order = orderRepository.findByOrderNo(orderNo)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.NOT_FOUND,
				"Order " + orderNo + " was not found"
			));

		// 这里就是支付状态机的一次流转：
		// 只允许从 PENDING 进入 SUCCESS。
		validateStatusTransition(paymentRecord.getStatus(), PaymentStatus.SUCCESS);
		paymentRecord.setStatus(PaymentStatus.SUCCESS);

		// 支付成功后，订单状态也同步从 CREATED 推进到 PAID。
		validateOrderStatusTransition(order.getStatus(), OrderStatus.PAID);
		order.setStatus(OrderStatus.PAID);

		paymentRecordRepository.save(paymentRecord);
		orderRepository.save(order);
	}

	private void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus targetStatus) {
		if (!currentStatus.canTransitionTo(targetStatus)) {
			throw new BusinessException(
				ErrorCode.BAD_REQUEST,
				"Payment status cannot transition from " + currentStatus + " to " + targetStatus
			);
		}
	}

	private void validateOrderStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
		if (!currentStatus.canTransitionTo(targetStatus)) {
			throw new BusinessException(
				ErrorCode.BAD_REQUEST,
				"Order status cannot transition from " + currentStatus + " to " + targetStatus
			);
		}
	}
}
