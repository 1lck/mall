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
	/** 支付成功事件的幂等处理标识。 */
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

	/**
	 * 消费支付成功事件，并把对应订单标记为已支付。
	 */
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
			log.info("支付成功事件已处理过，本次直接跳过: 订单号={}", event.orderNo());
			KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);
			return;
		}

		try {
			OrderEntity order = orderRepository.findByOrderNo(event.orderNo())
				.orElseThrow(() -> new BusinessException(
					ErrorCode.NOT_FOUND,
					"订单号为 " + event.orderNo() + " 的订单不存在"
				));

			validateOrderStatusTransition(order.getStatus(), OrderStatus.PAID);
			order.setStatus(OrderStatus.PAID);
			orderRepository.save(order);

			log.info("支付成功事件消费完成，订单已标记为已支付: 订单号={}", event.orderNo());
			// 订单状态落库成功后，再由 afterCommit 回调确认 Kafka offset。
			KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);
		} catch (BusinessException exception) {
			// 业务异常通常重试也不会改变结果，比如订单不存在、状态流转不合法。
			// 这里选择记录并结束消费，避免同一条坏消息被无限重放。
			log.warn(
				"支付成功事件属于不可重试的业务异常，当前消息将直接丢弃: 订单号={}, 错误类型={}, 错误信息={}",
				event.orderNo(),
				describeErrorCode(exception.getErrorCode()),
				exception.getMessage()
			);
			KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);
		}
	}

	/**
	 * 校验订单状态是否允许流转到目标状态。
	 */
	private void validateOrderStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
		if (!currentStatus.canTransitionTo(targetStatus)) {
			throw new BusinessException(
				ErrorCode.BAD_REQUEST,
				"订单状态不允许从" + describeOrderStatus(currentStatus) + "流转到" + describeOrderStatus(targetStatus)
			);
		}
	}

	/**
	 * 把订单状态枚举转换成便于日志和异常阅读的中文描述。
	 */
	private String describeOrderStatus(OrderStatus status) {
		return switch (status) {
			case CREATED -> "待支付";
			case PAID -> "已支付";
			case CANCELLED -> "已取消";
		};
	}

	/**
	 * 把业务错误码转换成日志里更易读的中文分类。
	 */
	private String describeErrorCode(ErrorCode errorCode) {
		return switch (errorCode) {
			case BAD_REQUEST -> "请求参数错误";
			case UNAUTHORIZED -> "未登录";
			case FORBIDDEN -> "无权限";
			case NOT_FOUND -> "资源不存在";
			case INTERNAL_ERROR -> "系统异常";
			case SUCCESS -> "成功";
		};
	}
}
