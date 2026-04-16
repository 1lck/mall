package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.order.event.OrderCreatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaMessagingConfigTests {

	@Test
	void orderCreatedConsumerFactoryShouldWrapValueDeserializerWithErrorHandlingDeserializer() {
		KafkaMessagingConfig config = new KafkaMessagingConfig();
		KafkaProperties kafkaProperties = new KafkaProperties();
		kafkaProperties.setBootstrapServers(List.of("localhost:9094"));

		DefaultKafkaConsumerFactory<String, OrderCreatedEvent> consumerFactory =
			(DefaultKafkaConsumerFactory<String, OrderCreatedEvent>) config.orderCreatedConsumerFactory(kafkaProperties);

		assertThat(consumerFactory.getKeyDeserializer()).isInstanceOf(ErrorHandlingDeserializer.class);
		assertThat(consumerFactory.getValueDeserializer()).isInstanceOf(ErrorHandlingDeserializer.class);
	}

	@Test
	void orderCreatedKafkaListenerContainerFactoryShouldRegisterCommonErrorHandler() throws Exception {
		KafkaMessagingConfig config = new KafkaMessagingConfig();
		ConsumerFactory<String, OrderCreatedEvent> consumerFactory = mock(ConsumerFactory.class);

		ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
			config.orderCreatedKafkaListenerContainerFactory(consumerFactory);

		assertThat(readCommonErrorHandler(factory)).isNotNull();
	}

	@Test
	void orderCreatedTopicShouldUsePartitionCountFromProperties() {
		KafkaMessagingConfig config = new KafkaMessagingConfig();
		KafkaTopicsProperties properties = new KafkaTopicsProperties();
		KafkaTopicsProperties.Topics topics = new KafkaTopicsProperties.Topics();
		KafkaTopicsProperties.Partitions partitions = new KafkaTopicsProperties.Partitions();
		topics.setOrderCreated("mall.order.created");
		partitions.setOrderCreated(3);
		properties.setTopics(topics);
		properties.setPartitions(partitions);

		NewTopic topic = config.orderCreatedTopic(properties);

		assertThat(topic.name()).isEqualTo("mall.order.created");
		assertThat(topic.numPartitions()).isEqualTo(3);
	}

	/**
	 * 通过反射读取监听器工厂上挂载的通用错误处理器，避免为了测试而侵入生产代码。
	 */
	private CommonErrorHandler readCommonErrorHandler(
		ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory
	) throws Exception {
		Field field = factory.getClass().getSuperclass().getDeclaredField("commonErrorHandler");
		field.setAccessible(true);
		return (CommonErrorHandler) field.get(factory);
	}
}
