package com.mall.infrastructure.messaging.kafka;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.persistence.entity.OrderEntity;
import com.mall.modules.order.persistence.mapper.OrderEventRecordMapper;
import com.mall.modules.order.persistence.mapper.OrderMapper;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付成功事件消费者。
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class PaymentSucceededOrderConsumer {

	private static final Logger log = LoggerFactory.getLogger(PaymentSucceededOrderConsumer.class);
	private static final String PAYMENT_SUCCEEDED_EVENT_TYPE = "PAYMENT_SUCCEEDED";

	private final OrderMapper orderRepository;
	private final OrderEventRecordMapper orderEventRecordRepository;

	public PaymentSucceededOrderConsumer(
		OrderMapper orderRepository,
		OrderEventRecordMapper orderEventRecordRepository
	) {
		this.orderRepository = orderRepository;
		this.orderEventRecordRepository = orderEventRecordRepository;
	}

	@KafkaListener(
		topics = "${mall.kafka.topics.payment-succeeded}",
		groupId = "${mall.kafka.consumer-group}",
		containerFactory = "paymentSucceededKafkaListenerContainerFactory"
	)
	@Transactional
	public void onPaymentSucceeded(PaymentSucceededEvent event, Acknowledgment acknowledgment) {
		// 支付成功这半段和订单创建那半段保持同一种并发模型：
		// 先抢处理资格，再做状态回写，避免同一笔支付事件被并发重复生效。
		boolean claimed = orderEventRecordRepository.claimProcessing(
			PAYMENT_SUCCEEDED_EVENT_TYPE,
			event.orderNo()
		) == 1;
		if (!claimed) {
			// 即使只是“跳过重复事件”，也统一在事务完成后再 ack，避免两套确认时序。
			log.info("Kafka payment succeeded event skipped because it was already processed: orderNo={}", event.orderNo());
			KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);
			return;
		}

		try {
			OrderEntity order = orderRepository.findByOrderNo(event.orderNo())
				.orElseThrow(() -> new BusinessException(
					ErrorCode.NOT_FOUND,
					"Order " + event.orderNo() + " was not found"
				));

			validateOrderStatusTransition(order.getStatus(), OrderStatus.PAID);
			order.setStatus(OrderStatus.PAID);
			orderRepository.save(order);

			log.info("Kafka payment succeeded event consumed and order marked paid: orderNo={}", event.orderNo());
			// 订单状态落库成功后，再由 afterCommit 回调确认 Kafka offset。
			KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);
		} catch (BusinessException exception) {
			// 业务异常通常重试也不会改变结果，比如订单不存在、状态流转不合法。
			// 这里选择记录并结束消费，避免同一条坏消息被无限重放。
			log.warn(
				"Kafka payment succeeded event dropped as non-retryable business error: orderNo={}, code={}, message={}",
				event.orderNo(),
				exception.getErrorCode(),
				exception.getMessage()
			);
			KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);
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
