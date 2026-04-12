package com.mall.infrastructure.messaging.kafka;

import com.mall.modules.order.application.OrderEventPublisher;
import com.mall.modules.order.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka 关闭时使用的空实现。
 *
 * <p>它的作用是：
 * 即使你本地还没起 Kafka，Spring 里也依然有一个可用的 OrderEventPublisher，
 * 这样业务代码不会因为缺少消息中间件就直接启动失败。</p>
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOrderEventPublisher implements OrderEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(NoOpOrderEventPublisher.class);

	@Override
	public void publishOrderCreated(OrderCreatedEvent event) {
		// 这里故意什么都不发，只打日志。
		// 这样可以把“业务代码能跑”与“Kafka 环境是否已就绪”拆开处理。
		log.info(
			"Kafka is disabled, skip publishing order created event: orderId={}, orderNo={}",
			event.orderId(),
			event.orderNo()
		);
	}
}
