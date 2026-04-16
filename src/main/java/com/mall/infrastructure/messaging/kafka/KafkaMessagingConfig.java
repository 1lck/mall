package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.order.event.OrderCreatedEvent;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonDelegatingErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.util.backoff.FixedBackOff;

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

	private static final Logger log = LoggerFactory.getLogger(KafkaMessagingConfig.class);

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

		// 反序列化失败如果直接从 poll 阶段抛出，监听器甚至拿不到这条消息，
		// 也无法交给统一错误处理器跳过坏消息，所以这里统一套一层 ErrorHandlingDeserializer。
		ErrorHandlingDeserializer<String> keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());
		keyDeserializer.setForKey(true);
		ErrorHandlingDeserializer<OrderCreatedEvent> safeValueDeserializer =
			new ErrorHandlingDeserializer<>(valueDeserializer);
		return new DefaultKafkaConsumerFactory<>(consumerProperties, keyDeserializer, safeValueDeserializer);
	}

	/**
	 * 创建订单创建事件监听器工厂。
	 */
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderCreatedKafkaListenerContainerFactory(
		ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory,
		KafkaTopicsProperties kafkaTopicsProperties
	) {
		// 监听器工厂就是给 @KafkaListener 用的“消费容器模板”。
		// 我们这里单独配一份给 OrderCreatedEvent，后面读起来会更清楚。
		ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
			new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(orderCreatedConsumerFactory);
		factory.setCommonErrorHandler(kafkaCommonErrorHandler());
		factory.setConcurrency(kafkaTopicsProperties.getConcurrency().getOrderCreated());

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
		ErrorHandlingDeserializer<String> keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());
		keyDeserializer.setForKey(true);
		ErrorHandlingDeserializer<PaymentSucceededEvent> safeValueDeserializer =
			new ErrorHandlingDeserializer<>(valueDeserializer);
		return new DefaultKafkaConsumerFactory<>(consumerProperties, keyDeserializer, safeValueDeserializer);
	}

	/**
	 * 创建支付成功事件监听器工厂。
	 */
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, PaymentSucceededEvent> paymentSucceededKafkaListenerContainerFactory(
		ConsumerFactory<String, PaymentSucceededEvent> paymentSucceededConsumerFactory,
		KafkaTopicsProperties kafkaTopicsProperties
	) {
		ConcurrentKafkaListenerContainerFactory<String, PaymentSucceededEvent> factory =
			new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(paymentSucceededConsumerFactory);
		factory.setCommonErrorHandler(kafkaCommonErrorHandler());
		factory.setConcurrency(kafkaTopicsProperties.getConcurrency().getPaymentSucceeded());
		// 支付成功事件也使用手动确认，便于后续扩展重试和补偿逻辑。
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
		return factory;
	}

	/**
	 * 创建 Kafka 消费通用错误处理器。
	 */
	@Bean
	public CommonErrorHandler kafkaCommonErrorHandler() {
		// 绝大多数运行期异常仍然保持“持续重试”的消费语义，
		// 避免因为一次短暂故障就把真实业务消息直接跳过。
		DefaultErrorHandler defaultErrorHandler =
			new DefaultErrorHandler(new FixedBackOff(1000L, FixedBackOff.UNLIMITED_ATTEMPTS));

		// 反序列化坏消息属于“内容本身有问题”，重试通常也不会自愈，
		// 因此这里首错就记录并跳过，避免消费者长期卡死在同一个 offset。
		ConsumerRecordRecoverer recoverer = (record, exception) -> log.error(
			"Kafka 消息反序列化失败，已跳过坏消息: topic={}, partition={}, offset={}, key={}, 错误信息={}",
			record.topic(),
			record.partition(),
			record.offset(),
			record.key(),
			exception.getMessage()
		);
		DefaultErrorHandler deserializationErrorHandler =
			new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));

		CommonDelegatingErrorHandler delegatingErrorHandler =
			new CommonDelegatingErrorHandler(defaultErrorHandler);
		delegatingErrorHandler.setCauseChainTraversing(true);
		delegatingErrorHandler.addDelegate(DeserializationException.class, deserializationErrorHandler);
		return delegatingErrorHandler;
	}

	/**
	 * 确保订单创建事件 topic 在启动时存在。
	 */
	@Bean
	public NewTopic orderCreatedTopic(KafkaTopicsProperties kafkaTopicsProperties) {
		// 启动时自动确保 topic 存在，避免你第一次联调时还得手动创建主题。
		return TopicBuilder
			.name(kafkaTopicsProperties.getTopics().getOrderCreated())
			.partitions(kafkaTopicsProperties.getPartitions().getOrderCreated())
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
			.partitions(kafkaTopicsProperties.getPartitions().getPaymentSucceeded())
			.replicas(1)
			.build();
	}
}
