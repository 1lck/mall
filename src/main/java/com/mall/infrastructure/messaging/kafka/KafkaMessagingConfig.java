package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.order.event.OrderCreatedEvent;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.config.TopicBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 消息基础配置。
 *
 * <p>第一阶段我们先把它控制在“够用”：
 * 1. 能发送 JSON 消息
 * 2. 能消费 OrderCreatedEvent
 * 3. 能自动创建 orderCreated topic</p>
 */
@Configuration
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class KafkaMessagingConfig {

	/**
	 * 创建订单创建事件消费者工厂。
	 */
	@Bean
	public ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory(KafkaProperties kafkaProperties) {
		// 先把 Spring Boot 自动装配出来的 Kafka consumer 配置拿过来，
		// 再补上我们这个事件类型自己的反序列化规则。
		Map<String, Object> consumerProperties = new HashMap<>(kafkaProperties.buildConsumerProperties());

		// 这里告诉 Spring Kafka：
		// 收到的消息 value 按 JSON 反序列化成 OrderCreatedEvent。
		JsonDeserializer<OrderCreatedEvent> valueDeserializer = new JsonDeserializer<>(OrderCreatedEvent.class);
		valueDeserializer.addTrustedPackages("com.mall.modules.order.event");
		return new DefaultKafkaConsumerFactory<>(consumerProperties, new StringDeserializer(), valueDeserializer);
	}

	/**
	 * 创建订单创建事件监听器工厂。
	 */
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderCreatedKafkaListenerContainerFactory(
		ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory
	) {
		// 监听器工厂就是给 @KafkaListener 用的“消费容器模板”。
		// 我们这里单独配一份给 OrderCreatedEvent，后面读起来会更清楚。
		ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
			new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(orderCreatedConsumerFactory);

		// 手动确认消息。
		// 第一阶段虽然消费者只是打日志，但先把确认模式定成 manual，
		// 后面练重试、补偿时不需要再换一套消费模型。
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
		return factory;
	}

	/**
	 * 创建支付成功事件消费者工厂。
	 */
	@Bean
	public ConsumerFactory<String, PaymentSucceededEvent> paymentSucceededConsumerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> consumerProperties = new HashMap<>(kafkaProperties.buildConsumerProperties());
		JsonDeserializer<PaymentSucceededEvent> valueDeserializer = new JsonDeserializer<>(PaymentSucceededEvent.class);
		valueDeserializer.addTrustedPackages("com.mall.modules.payment.event");
		return new DefaultKafkaConsumerFactory<>(consumerProperties, new StringDeserializer(), valueDeserializer);
	}

	/**
	 * 创建支付成功事件监听器工厂。
	 */
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, PaymentSucceededEvent> paymentSucceededKafkaListenerContainerFactory(
		ConsumerFactory<String, PaymentSucceededEvent> paymentSucceededConsumerFactory
	) {
		ConcurrentKafkaListenerContainerFactory<String, PaymentSucceededEvent> factory =
			new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(paymentSucceededConsumerFactory);
		// 支付成功事件也使用手动确认，便于后续扩展重试和补偿逻辑。
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
		return factory;
	}

	/**
	 * 确保订单创建事件 topic 在启动时存在。
	 */
	@Bean
	public NewTopic orderCreatedTopic(KafkaTopicsProperties kafkaTopicsProperties) {
		// 启动时自动确保 topic 存在，避免你第一次联调时还得手动创建主题。
		return TopicBuilder
			.name(kafkaTopicsProperties.getTopics().getOrderCreated())
			.partitions(1)
			.replicas(1)
			.build();
	}

	/**
	 * 确保支付成功事件 topic 在启动时存在。
	 */
	@Bean
	public NewTopic paymentSucceededTopic(KafkaTopicsProperties kafkaTopicsProperties) {
		return TopicBuilder
			.name(kafkaTopicsProperties.getTopics().getPaymentSucceeded())
			.partitions(1)
			.replicas(1)
			.build();
	}
}
