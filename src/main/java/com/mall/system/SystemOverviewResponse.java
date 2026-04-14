package com.mall.system;

import java.util.List;

/**
 * 系统概览接口的响应对象。
 */
public record SystemOverviewResponse(
	/** 应用名称。 */
	String applicationName,
	/** 已启用的模块列表。 */
	List<String> modules,
	/** Kafka 配置视图。 */
	KafkaView kafka
) {

	/**
	 * Kafka 相关的简化视图。
	 */
	public record KafkaView(
		/** 当前是否启用了 Kafka。 */
		boolean enabled,
		/** Kafka 服务地址。 */
		String bootstrapServers,
		/** 消费者组名称。 */
		String consumerGroup,
		/** 已配置的业务 topic 列表。 */
		List<String> topics
	) {
	}
}
