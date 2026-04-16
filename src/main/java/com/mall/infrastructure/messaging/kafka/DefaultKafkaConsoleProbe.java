package com.mall.infrastructure.messaging.kafka;

import com.mall.config.KafkaTopicsProperties;
import com.mall.system.KafkaConsoleProbe;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * 基于 Kafka AdminClient 和原生 Consumer 的控制台探针实现。
 *
 * <p>这个实现只做“观测”，不在这里修改业务 offset 或消息内容，
 * 避免调试页直接影响当前业务链路。</p>
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class DefaultKafkaConsoleProbe implements KafkaConsoleProbe {

	private static final Duration KAFKA_TIMEOUT = Duration.ofSeconds(3);

	private final KafkaTopicsProperties kafkaTopicsProperties;

	public DefaultKafkaConsoleProbe(KafkaTopicsProperties kafkaTopicsProperties) {
		this.kafkaTopicsProperties = kafkaTopicsProperties;
	}

	/**
	 * 读取指定 topic 的运行状态。
	 */
	@Override
	public List<TopicRuntimeSnapshot> inspectTopics(List<String> topics, String consumerGroup) {
		if (topics.isEmpty()) {
			return List.of();
		}

		try (AdminClient adminClient = AdminClient.create(adminClientProperties())) {
			Map<TopicPartition, OffsetAndMetadata> groupOffsets = loadGroupOffsets(adminClient, consumerGroup);
			Map<String, org.apache.kafka.common.KafkaFuture<TopicDescription>> futures =
				adminClient.describeTopics(topics).topicNameValues();
			List<TopicRuntimeSnapshot> result = new ArrayList<>();
			for (String topic : topics) {
				result.add(describeSingleTopic(adminClient, futures.get(topic), topic, groupOffsets));
			}
			return result;
		}
	}

	/**
	 * 读取指定 topic 最近消息。
	 */
	@Override
	public List<KafkaRecordSnapshot> fetchRecentMessages(String topic, Integer partition, int limit) {
		Properties properties = consumerProperties();
		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
			List<TopicPartition> topicPartitions = resolveTopicPartitions(consumer, topic, partition);
			if (topicPartitions.isEmpty()) {
				return List.of();
			}

			consumer.assign(topicPartitions);
			Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);
			for (TopicPartition topicPartition : topicPartitions) {
				long startOffset = Math.max(endOffsets.getOrDefault(topicPartition, 0L) - limit, 0L);
				consumer.seek(topicPartition, startOffset);
			}

			List<KafkaRecordSnapshot> records = new ArrayList<>();
			for (int attempt = 0; attempt < 3; attempt++) {
				var polled = consumer.poll(KAFKA_TIMEOUT);
				polled.forEach(record -> records.add(new KafkaRecordSnapshot(
					record.topic(),
					record.partition(),
					record.offset(),
					record.timestamp(),
					record.key(),
					record.value()
				)));
				if (polled.isEmpty()) {
					break;
				}
			}

			return records.stream()
				.sorted(Comparator.comparingLong(KafkaRecordSnapshot::timestamp).reversed()
					.thenComparing(KafkaRecordSnapshot::offset, Comparator.reverseOrder()))
				.limit(limit)
				.toList();
		}
	}

	/**
	 * 读取单个 topic 的分区位点信息。
	 */
	private TopicRuntimeSnapshot describeSingleTopic(
		AdminClient adminClient,
		org.apache.kafka.common.KafkaFuture<TopicDescription> future,
		String topic,
		Map<TopicPartition, OffsetAndMetadata> groupOffsets
	) {
		if (future == null) {
			return new TopicRuntimeSnapshot(topic, false, List.of());
		}

		try {
			TopicDescription description = future.get();
			Map<TopicPartition, OffsetSpec> latestOffsetsQuery = new HashMap<>();
			for (TopicPartitionInfo partition : description.partitions().stream()
				.map(item -> new TopicPartitionInfo(item.partition()))
				.toList()) {
				latestOffsetsQuery.put(new TopicPartition(topic, partition.partition()), OffsetSpec.latest());
			}

			Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
				adminClient.listOffsets(latestOffsetsQuery).all().get();
			List<PartitionRuntimeSnapshot> partitions = description.partitions().stream()
				.map(item -> {
					TopicPartition topicPartition = new TopicPartition(topic, item.partition());
					ListOffsetsResult.ListOffsetsResultInfo endOffsetInfo = endOffsets.get(topicPartition);
					long endOffset = endOffsetInfo == null ? 0L : endOffsetInfo.offset();
					OffsetAndMetadata committed = groupOffsets.get(topicPartition);
					return new PartitionRuntimeSnapshot(
						item.partition(),
						endOffset,
						committed == null ? null : committed.offset()
					);
				})
				.sorted(Comparator.comparingInt(PartitionRuntimeSnapshot::partition))
				.toList();
			return new TopicRuntimeSnapshot(topic, true, partitions);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return new TopicRuntimeSnapshot(topic, false, List.of());
		} catch (ExecutionException exception) {
			return new TopicRuntimeSnapshot(topic, false, List.of());
		}
	}

	/**
	 * 查询当前消费组的已提交位点。
	 */
	private Map<TopicPartition, OffsetAndMetadata> loadGroupOffsets(AdminClient adminClient, String consumerGroup) {
		try {
			return adminClient.listConsumerGroupOffsets(consumerGroup)
				.partitionsToOffsetAndMetadata()
				.get();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return Map.of();
		} catch (ExecutionException exception) {
			return Map.of();
		}
	}

	/**
	 * 解析本次要读取的分区列表。
	 */
	private List<TopicPartition> resolveTopicPartitions(
		KafkaConsumer<String, String> consumer,
		String topic,
		Integer partition
	) {
		List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic, KAFKA_TIMEOUT);
		if (partitionInfos == null || partitionInfos.isEmpty()) {
			return List.of();
		}

		return partitionInfos.stream()
			.filter(item -> partition == null || item.partition() == partition)
			.map(item -> new TopicPartition(topic, item.partition()))
			.sorted(Comparator.comparingInt(TopicPartition::partition))
			.toList();
	}

	/**
	 * 创建 AdminClient 配置。
	 */
	private Map<String, Object> adminClientProperties() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("bootstrap.servers", kafkaTopicsProperties.getBootstrapServers());
		return properties;
	}

	/**
	 * 创建用于只读消息预览的消费者配置。
	 */
	private Properties consumerProperties() {
		Properties properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTopicsProperties.getBootstrapServers());
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, "mall-kafka-console-" + UUID.randomUUID());
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		return properties;
	}

	/**
	 * 只保留分区编号，避免在 lambda 里来回处理 Kafka 自带复杂类型。
	 */
	private record TopicPartitionInfo(int partition) {
	}
}
