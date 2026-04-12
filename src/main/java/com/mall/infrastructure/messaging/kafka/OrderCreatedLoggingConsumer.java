package com.mall.infrastructure.messaging.kafka;

import com.mall.modules.order.event.OrderCreatedEvent;
import com.mall.modules.order.persistence.OrderEventRecordEntity;
import com.mall.modules.order.persistence.OrderEventRecordRepository;
import com.mall.modules.payment.domain.PaymentStatus;
import com.mall.modules.payment.persistence.PaymentRecordEntity;
import com.mall.modules.payment.persistence.PaymentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第一阶段只做最小消费者。
 *
 * <p>它暂时不做库存扣减、不做支付编排，
 * 只负责帮我们确认一件事：
 * “订单创建事件已经真的从 Kafka 被消费到了。”</p>
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class OrderCreatedLoggingConsumer {

	private static final Logger log = LoggerFactory.getLogger(OrderCreatedLoggingConsumer.class);
	private static final String ORDER_CREATED_EVENT_TYPE = "ORDER_CREATED";
	private final OrderEventRecordRepository orderEventRecordRepository;
	private final PaymentRecordRepository paymentRecordRepository;

	public OrderCreatedLoggingConsumer(
		OrderEventRecordRepository orderEventRecordRepository,
		PaymentRecordRepository paymentRecordRepository
	) {
		this.orderEventRecordRepository = orderEventRecordRepository;
		this.paymentRecordRepository = paymentRecordRepository;
	}

	@KafkaListener(
		topics = "${mall.kafka.topics.order-created}",
		groupId = "${mall.kafka.consumer-group}",
		containerFactory = "orderCreatedKafkaListenerContainerFactory"
	)
	@Transactional
	public void onOrderCreated(OrderCreatedEvent event, Acknowledgment acknowledgment) {
		// 先做一层应用侧幂等判断：
		// 如果这条“订单创建事件”已经处理过，就直接确认消息，不再重复入库。
		boolean alreadyProcessed = orderEventRecordRepository.existsByEventTypeAndOrderNo(
			ORDER_CREATED_EVENT_TYPE,
			event.orderNo()
		);
		if (alreadyProcessed) {
			log.info("Kafka order created event skipped because it was already processed: orderNo={}", event.orderNo());
			acknowledgment.acknowledge();
			return;
		}

		// 这一步开始接入一个最小支付流程：
		// 当订单创建事件第一次被消费时，先为这笔订单生成一条待支付记录。
		PaymentRecordEntity paymentRecord = new PaymentRecordEntity();
		paymentRecord.setOrderNo(event.orderNo());
		paymentRecord.setAmount(event.totalAmount());
		paymentRecord.setStatus(PaymentStatus.PENDING);
		paymentRecordRepository.save(paymentRecord);

		// 第一阶段先把“消费者真的做了一次业务处理”落下来：
		// 当前这里表示：支付记录已经创建成功，这条订单创建消息也可以记为已处理。
		OrderEventRecordEntity record = new OrderEventRecordEntity();
		record.setEventType(ORDER_CREATED_EVENT_TYPE);
		record.setOrderNo(event.orderNo());
		orderEventRecordRepository.save(record);

		// 当前真实业务仍然保持极简：
		// 先创建支付记录、再记录消费痕迹，方便你观察消息驱动的后续动作已经开始发生。
		log.info(
			"Kafka order created event consumed and payment record created: orderId={}, orderNo={}, userId={}, totalAmount={}",
			event.orderId(),
			event.orderNo(),
			event.userId(),
			event.totalAmount()
		);

		// 最后手动确认，表示这条消息已经被成功处理。
		acknowledgment.acknowledge();
	}
}
