package com.mall.infrastructure.messaging.kafka;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.persistence.OrderEntity;
import com.mall.modules.order.persistence.OrderEventRecordEntity;
import com.mall.modules.order.persistence.OrderEventRecordRepository;
import com.mall.modules.order.persistence.OrderRepository;
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

	private final OrderRepository orderRepository;
	private final OrderEventRecordRepository orderEventRecordRepository;

	public PaymentSucceededOrderConsumer(
		OrderRepository orderRepository,
		OrderEventRecordRepository orderEventRecordRepository
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
		boolean alreadyProcessed = orderEventRecordRepository.existsByEventTypeAndOrderNo(
			PAYMENT_SUCCEEDED_EVENT_TYPE,
			event.orderNo()
		);
		if (alreadyProcessed) {
			log.info("Kafka payment succeeded event skipped because it was already processed: orderNo={}", event.orderNo());
			acknowledgment.acknowledge();
			return;
		}

		OrderEntity order = orderRepository.findByOrderNo(event.orderNo())
			.orElseThrow(() -> new BusinessException(
				ErrorCode.NOT_FOUND,
				"Order " + event.orderNo() + " was not found"
			));

		validateOrderStatusTransition(order.getStatus(), OrderStatus.PAID);
		order.setStatus(OrderStatus.PAID);
		orderRepository.save(order);

		OrderEventRecordEntity record = new OrderEventRecordEntity();
		record.setEventType(PAYMENT_SUCCEEDED_EVENT_TYPE);
		record.setOrderNo(event.orderNo());
		orderEventRecordRepository.save(record);

		log.info("Kafka payment succeeded event consumed and order marked paid: orderNo={}", event.orderNo());
		acknowledgment.acknowledge();
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
