package com.mall.infrastructure.messaging.kafka;

import com.mall.modules.order.event.OrderCreatedEvent;
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

	@KafkaListener(
		topics = "${mall.kafka.topics.order-created}",
		groupId = "${mall.kafka.consumer-group}",
		containerFactory = "orderCreatedKafkaListenerContainerFactory"
	)
	public void onOrderCreated(OrderCreatedEvent event, Acknowledgment acknowledgment) {
		// 第一阶段消费逻辑先极简：
		// 收到消息 -> 打日志 -> 手动 ack。
		// 等链路跑通后，再逐步替换成真正的库存/支付/订单超时逻辑。
		log.info(
			"Kafka order created event consumed: orderId={}, orderNo={}, userId={}, totalAmount={}",
			event.orderId(),
			event.orderNo(),
			event.userId(),
			event.totalAmount()
		);

		// 手动确认，表示这条消息已经被成功处理。
		acknowledgment.acknowledge();
	}
}
