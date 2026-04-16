package com.mall.system;

import java.util.List;

/**
 * Kafka 控制台探针接口。
 *
 * <p>管理后台不直接依赖 Kafka AdminClient 和 Consumer 的细节，
 * 而是通过这个探针读取 topic 运行态和最近消息，方便测试和后续替换实现。</p>
 */
public interface KafkaConsoleProbe {

	/**
	 * 读取指定 topic 列表在某个消费组下的运行状态快照。
	 *
	 * @param topics 要观察的 topic 列表
	 * @param consumerGroup 要查看提交位点的消费者组
	 * @return 每个 topic 的运行态结果
	 */
	List<TopicRuntimeSnapshot> inspectTopics(List<String> topics, String consumerGroup);

	/**
	 * 读取指定 topic 最近一批消息。
	 *
	 * @param topic topic 名称
	 * @param partition 可选分区过滤；为空时汇总全部分区
	 * @param limit 最多返回多少条消息
	 * @return 最近消息快照列表
	 */
	List<KafkaRecordSnapshot> fetchRecentMessages(String topic, Integer partition, int limit);

	/**
	 * 单个 topic 的运行态快照。
	 */
	record TopicRuntimeSnapshot(
		/** topic 名称。 */
		String topic,
		/** topic 当前是否真实存在。 */
		boolean exists,
		/** 各分区的位点快照。 */
		List<PartitionRuntimeSnapshot> partitions
	) {
	}

	/**
	 * 单个分区的位点快照。
	 */
	record PartitionRuntimeSnapshot(
		/** 分区编号。 */
		int partition,
		/** 当前分区的末尾 offset。 */
		long endOffset,
		/** 当前消费组已提交的 offset；为空表示还没有提交记录。 */
		Long committedOffset
	) {
	}

	/**
	 * Kafka 最近消息快照。
	 */
	record KafkaRecordSnapshot(
		/** topic 名称。 */
		String topic,
		/** 分区编号。 */
		int partition,
		/** 消息 offset。 */
		long offset,
		/** Kafka 原始时间戳。 */
		long timestamp,
		/** 消息 key。 */
		String key,
		/** 消息 value。 */
		String value
	) {
	}
}
