package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.payment.application.PaymentEventPublisher;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Kafka 的支付事件发布器。
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final KafkaTopicsProperties kafkaTopicsProperties;

	public KafkaPaymentEventPublisher(
		KafkaTemplate<String, Object> kafkaTemplate,
		KafkaTopicsProperties kafkaTopicsProperties
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.kafkaTopicsProperties = kafkaTopicsProperties;
	}

	@Override
	public void publishPaymentSucceeded(PaymentSucceededEvent event) {
		// topic 名不直接写死在代码里，而是统一从配置中读取。
		// 这样后面如果你想改 topic，只需要改 yml，不用改业务代码。
		String topic = kafkaTopicsProperties.getTopics().getPaymentSucceeded();

		// 真正发送 Kafka 消息的就是这句：
		// 第一个参数是 topic，
		// 第二个参数是消息 key，这里继续用 orderNo，方便按订单维度观察消息，
		// 第三个参数才是真正的消息体，也就是 PaymentSucceededEvent。
		kafkaTemplate.send(topic, event.orderNo(), event);

		// 发完后打一条日志，便于你在本地联调时确认：
		// “支付成功事件已经从生产者发出去了”。
		log.info("Kafka payment succeeded event published: topic={}, orderNo={}", topic, event.orderNo());
	}
}
