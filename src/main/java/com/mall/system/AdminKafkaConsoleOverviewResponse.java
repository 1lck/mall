package com.mall.system;

import java.util.List;

/**
 * Kafka 控制台总览响应。
 */
public record AdminKafkaConsoleOverviewResponse(
	/** 当前是否启用了 Kafka。 */
	boolean enabled,
	/** Kafka 服务地址。 */
	String bootstrapServers,
	/** 当前观察的消费者组。 */
	String consumerGroup,
	/** 顶部摘要信息。 */
	Summary summary,
	/** 各 topic 的运行状态。 */
	List<TopicView> topics
) {

	/**
	 * 顶部摘要信息。
	 */
	public record Summary(
		/** 当前观察的 topic 数量。 */
		int topicCount,
		/** 已存在的 topic 数量。 */
		int existingTopicCount,
		/** 处于积压或未分配状态的 topic 数量。 */
		int laggingTopicCount,
		/** 已存在 topic 的总分区数。 */
		long totalPartitionCount,
		/** 当前消费组总 lag。 */
		long totalLag
	) {
	}

	/**
	 * 单个 topic 的运行状态。
	 */
	public record TopicView(
		/** topic 名称。 */
		String topicName,
		/** 配置里期望的分区数；为空表示当前没有专门配置。 */
		Integer configuredPartitions,
		/** topic 是否已在 Kafka 中存在。 */
		boolean exists,
		/** 实际分区数。 */
		int actualPartitions,
		/** topic 汇总状态。 */
		String status,
		/** topic 总 lag。 */
		long totalLag,
		/** 分区详情。 */
		List<PartitionView> partitions
	) {
	}

	/**
	 * 单个分区的消费进度。
	 */
	public record PartitionView(
		/** 分区编号。 */
		int partition,
		/** 分区末尾 offset。 */
		long endOffset,
		/** 当前消费组已提交的 offset。 */
		Long committedOffset,
		/** 当前 lag。 */
		long lag,
		/** 分区状态。 */
		String status
	) {
	}
}
