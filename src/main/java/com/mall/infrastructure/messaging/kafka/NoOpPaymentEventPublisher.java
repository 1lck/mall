package com.mall.infrastructure.messaging.kafka;

import com.mall.modules.payment.application.PaymentEventPublisher;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka 关闭时的支付事件空实现。
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPaymentEventPublisher implements PaymentEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(NoOpPaymentEventPublisher.class);

	@Override
	public void publishPaymentSucceeded(PaymentSucceededEvent event) {
		log.info("消息队列未开启，跳过支付成功事件发布: 订单号={}", event.orderNo());
	}
}
