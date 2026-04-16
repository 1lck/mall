package com.mall.infrastructure.messaging.kafka;

import com.mall.system.KafkaConsoleProbe;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka 关闭时使用的空探针实现。
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpKafkaConsoleProbe implements KafkaConsoleProbe {

	/**
	 * Kafka 关闭时不返回任何运行态数据。
	 */
	@Override
	public List<TopicRuntimeSnapshot> inspectTopics(List<String> topics, String consumerGroup) {
		return List.of();
	}

	/**
	 * Kafka 关闭时不返回任何消息预览。
	 */
	@Override
	public List<KafkaRecordSnapshot> fetchRecentMessages(String topic, Integer partition, int limit) {
		return List.of();
	}
}
