package com.mall.infrastructure.messaging.kafka;

import com.mall.modules.order.event.OrderCreatedEvent;
import com.mall.modules.order.persistence.OrderEventRecordEntity;
import com.mall.modules.order.persistence.OrderEventRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

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
	private final OrderEventRecordRepository orderEventRecordRepository;

	public OrderCreatedLoggingConsumer(OrderEventRecordRepository orderEventRecordRepository) {
		this.orderEventRecordRepository = orderEventRecordRepository;
	}

	@KafkaListener(
		topics = "${mall.kafka.topics.order-created}",
		groupId = "${mall.kafka.consumer-group}",
		containerFactory = "orderCreatedKafkaListenerContainerFactory"
	)
	public void onOrderCreated(OrderCreatedEvent event, Acknowledgment acknowledgment) {
		// 第一阶段先把“消费者真的做了一次业务处理”落下来：
		// 收到消息后先写一条消费记录到数据库。
		OrderEventRecordEntity record = new OrderEventRecordEntity();
		record.setEventType("ORDER_CREATED");
		record.setOrderNo(event.orderNo());
		orderEventRecordRepository.save(record);

		// 当前真实业务仍然保持极简：
		// 先记录消费痕迹，再打印日志，方便你观察消息已经被处理过。
		log.info(
			"Kafka order created event consumed: orderId={}, orderNo={}, userId={}, totalAmount={}",
			event.orderId(),
			event.orderNo(),
			event.userId(),
			event.totalAmount()
		);

		// 最后手动确认，表示这条消息已经被成功处理。
		acknowledgment.acknowledge();
	}
}
