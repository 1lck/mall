package com.mall.modules.payment.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.payment.domain.PaymentStatus;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import com.mall.modules.payment.persistence.entity.PaymentRecordEntity;
import com.mall.modules.payment.persistence.mapper.PaymentRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 支付应用服务实现。
 *
 * <p>当前先只实现最小版“支付成功”流转：
 * 按订单号找到支付记录，然后把状态从 PENDING 改成 SUCCESS，
 * 再把“支付成功”这件事发布给消息消费者。</p>
 */
@Service
@Transactional
public class DefaultPaymentApplicationService implements PaymentApplicationService {

	private final PaymentRecordMapper paymentRecordRepository;
	private final PaymentEventPublisher paymentEventPublisher;

	public DefaultPaymentApplicationService(
		PaymentRecordMapper paymentRecordRepository,
		PaymentEventPublisher paymentEventPublisher
	) {
		this.paymentRecordRepository = paymentRecordRepository;
		this.paymentEventPublisher = paymentEventPublisher;
	}

	/**
	 * 把指定订单的支付记录流转到支付成功状态，并发布支付成功事件。
	 */
	@Override
	public void markPaymentSuccess(String orderNo) {
		// 先查支付记录，查不到说明这笔订单还没创建支付任务。
		PaymentRecordEntity paymentRecord = paymentRecordRepository.findByOrderNo(orderNo)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.NOT_FOUND,
				"Payment record for order " + orderNo + " was not found"
			));

		// 这里就是支付状态机的一次流转：
		// 只允许从 PENDING 进入 SUCCESS。
		validateStatusTransition(paymentRecord.getStatus(), PaymentStatus.SUCCESS);
		paymentRecord.setStatus(PaymentStatus.SUCCESS);
		paymentRecordRepository.save(paymentRecord);
		paymentEventPublisher.publishPaymentSucceeded(new PaymentSucceededEvent(
			paymentRecord.getOrderNo(),
			paymentRecord.getAmount(),
			Instant.now()
		));
	}

	/**
	 * 校验支付状态是否允许流转到目标状态。
	 */
	private void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus targetStatus) {
		if (!currentStatus.canTransitionTo(targetStatus)) {
			throw new BusinessException(
				ErrorCode.BAD_REQUEST,
				"Payment status cannot transition from " + currentStatus + " to " + targetStatus
			);
		}
	}
}
