package com.mall.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.config.KafkaTopicsProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理后台 Kafka 控制台应用服务。
 *
 * <p>负责把底层 Kafka 运行态数据整理成前端更容易直接展示的视图结构，
 * 让页面可以聚焦“topic 健康度、lag 和最近消息”，而不是散落的底层字段。</p>
 */
@Service
public class AdminKafkaConsoleService {

	private static final String TOPIC_STATUS_MISSING = "MISSING";
	private static final String TOPIC_STATUS_HEALTHY = "HEALTHY";
	private static final String TOPIC_STATUS_LAGGING = "LAGGING";
	private static final String PARTITION_STATUS_CAUGHT_UP = "CAUGHT_UP";
	private static final String PARTITION_STATUS_LAGGING = "LAGGING";
	private static final String PARTITION_STATUS_UNASSIGNED = "UNASSIGNED";
	private static final String VALUE_FORMAT_EMPTY = "EMPTY";
	private static final String VALUE_FORMAT_JSON = "JSON";
	private static final String VALUE_FORMAT_TEXT = "TEXT";

	private final KafkaTopicsProperties kafkaTopicsProperties;
	private final KafkaConsoleProbe kafkaConsoleProbe;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AdminKafkaConsoleService(
		KafkaTopicsProperties kafkaTopicsProperties,
		KafkaConsoleProbe kafkaConsoleProbe
	) {
		this.kafkaTopicsProperties = kafkaTopicsProperties;
		this.kafkaConsoleProbe = kafkaConsoleProbe;
	}

	/**
	 * 返回 Kafka 控制台总览。
	 */
	public AdminKafkaConsoleOverviewResponse getOverview() {
		List<String> configuredTopics = configuredTopicNames();
		if (!kafkaTopicsProperties.isEnabled()) {
			return new AdminKafkaConsoleOverviewResponse(
				false,
				kafkaTopicsProperties.getBootstrapServers(),
				kafkaTopicsProperties.getConsumerGroup(),
				new AdminKafkaConsoleOverviewResponse.Summary(configuredTopics.size(), 0, 0, 0, 0),
				List.of()
			);
		}

		List<KafkaConsoleProbe.TopicRuntimeSnapshot> runtimeSnapshots =
			kafkaConsoleProbe.inspectTopics(configuredTopics, kafkaTopicsProperties.getConsumerGroup());
		Map<String, KafkaConsoleProbe.TopicRuntimeSnapshot> runtimeByTopic = runtimeSnapshots.stream()
			.collect(java.util.stream.Collectors.toMap(KafkaConsoleProbe.TopicRuntimeSnapshot::topic, item -> item));

		List<AdminKafkaConsoleOverviewResponse.TopicView> topics = new ArrayList<>();
		long totalLag = 0L;
		long totalPartitionCount = 0L;
		int existingTopicCount = 0;
		int laggingTopicCount = 0;

		for (String topicName : configuredTopics) {
			KafkaConsoleProbe.TopicRuntimeSnapshot runtime = runtimeByTopic.get(topicName);
			AdminKafkaConsoleOverviewResponse.TopicView topicView = toTopicView(topicName, runtime);
			topics.add(topicView);
			totalLag += topicView.totalLag();
			totalPartitionCount += topicView.actualPartitions();
			if (topicView.exists()) {
				existingTopicCount++;
			}
			if (TOPIC_STATUS_LAGGING.equals(topicView.status())) {
				laggingTopicCount++;
			}
		}

		return new AdminKafkaConsoleOverviewResponse(
			true,
			kafkaTopicsProperties.getBootstrapServers(),
			kafkaTopicsProperties.getConsumerGroup(),
			new AdminKafkaConsoleOverviewResponse.Summary(
				topics.size(),
				existingTopicCount,
				laggingTopicCount,
				totalPartitionCount,
				totalLag
			),
			topics
		);
	}

	/**
	 * 返回指定 topic 的最近消息。
	 */
	public AdminKafkaTopicMessagesResponse getTopicMessages(String topic, Integer partition, Integer limit) {
		String normalizedTopic = topic == null ? "" : topic.trim();
		if (normalizedTopic.isEmpty()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "topic 不能为空");
		}

		int safeLimit = limit == null ? 20 : Math.min(Math.max(limit, 1), 100);
		List<AdminKafkaTopicMessagesResponse.MessageView> messages = kafkaConsoleProbe
			.fetchRecentMessages(normalizedTopic, partition, safeLimit)
			.stream()
			.map(this::toMessageView)
			.toList();

		return new AdminKafkaTopicMessagesResponse(normalizedTopic, partition, safeLimit, messages);
	}

	/**
	 * 把底层 topic 运行态转换成前端可直接展示的结构。
	 */
	private AdminKafkaConsoleOverviewResponse.TopicView toTopicView(
		String topicName,
		KafkaConsoleProbe.TopicRuntimeSnapshot runtime
	) {
		if (runtime == null || !runtime.exists()) {
			return new AdminKafkaConsoleOverviewResponse.TopicView(
				topicName,
				configuredPartitions(topicName),
				false,
				0,
				TOPIC_STATUS_MISSING,
				0L,
				List.of()
			);
		}

		List<AdminKafkaConsoleOverviewResponse.PartitionView> partitions = runtime.partitions().stream()
			.map(this::toPartitionView)
			.toList();
		long totalLag = partitions.stream()
			.mapToLong(AdminKafkaConsoleOverviewResponse.PartitionView::lag)
			.sum();
		String status = partitions.stream().anyMatch(item -> !PARTITION_STATUS_CAUGHT_UP.equals(item.status()))
			? TOPIC_STATUS_LAGGING
			: TOPIC_STATUS_HEALTHY;

		return new AdminKafkaConsoleOverviewResponse.TopicView(
			topicName,
			configuredPartitions(topicName),
			true,
			partitions.size(),
			status,
			totalLag,
			partitions
		);
	}

	/**
	 * 把底层分区位点转换成前端状态标签。
	 */
	private AdminKafkaConsoleOverviewResponse.PartitionView toPartitionView(
		KafkaConsoleProbe.PartitionRuntimeSnapshot snapshot
	) {
		Long committedOffset = snapshot.committedOffset();
		long lag = committedOffset == null ? snapshot.endOffset() : Math.max(snapshot.endOffset() - committedOffset, 0L);
		String status;
		if (committedOffset == null) {
			status = PARTITION_STATUS_UNASSIGNED;
		} else if (lag > 0) {
			status = PARTITION_STATUS_LAGGING;
		} else {
			status = PARTITION_STATUS_CAUGHT_UP;
		}

		return new AdminKafkaConsoleOverviewResponse.PartitionView(
			snapshot.partition(),
			snapshot.endOffset(),
			committedOffset,
			lag,
			status
		);
	}

	/**
	 * 把消息快照转换成展示模型，并标注 value 格式。
	 */
	private AdminKafkaTopicMessagesResponse.MessageView toMessageView(KafkaConsoleProbe.KafkaRecordSnapshot record) {
		return new AdminKafkaTopicMessagesResponse.MessageView(
			record.topic(),
			record.partition(),
			record.offset(),
			Instant.ofEpochMilli(record.timestamp()).toString(),
			record.key(),
			record.value(),
			detectValueFormat(record.value())
		);
	}

	/**
	 * 返回当前控制台要观察的 topic 列表。
	 */
	private List<String> configuredTopicNames() {
		Set<String> topics = new LinkedHashSet<>();
		addTopic(topics, kafkaTopicsProperties.getTopics().getOrderCreated());
		addTopic(topics, kafkaTopicsProperties.getTopics().getPaymentSucceeded());
		return List.copyOf(topics);
	}

	/**
	 * 按 topic 名称找出配置里期望的分区数。
	 */
	private Integer configuredPartitions(String topicName) {
		if (topicName.equals(kafkaTopicsProperties.getTopics().getOrderCreated())) {
			return kafkaTopicsProperties.getPartitions().getOrderCreated();
		}
		if (topicName.equals(kafkaTopicsProperties.getTopics().getPaymentSucceeded())) {
			return kafkaTopicsProperties.getPartitions().getPaymentSucceeded();
		}
		return null;
	}

	/**
	 * 判断消息体更适合按 JSON 还是纯文本显示。
	 */
	private String detectValueFormat(String value) {
		if (value == null || value.isBlank()) {
			return VALUE_FORMAT_EMPTY;
		}

		try {
			var jsonNode = objectMapper.readTree(value);
			return (jsonNode.isObject() || jsonNode.isArray()) ? VALUE_FORMAT_JSON : VALUE_FORMAT_TEXT;
		} catch (Exception exception) {
			return VALUE_FORMAT_TEXT;
		}
	}

	/**
	 * 向观察列表里补充非空 topic。
	 */
	private void addTopic(Set<String> topics, String topic) {
		if (topic != null && !topic.isBlank()) {
			topics.add(topic);
		}
	}
}
