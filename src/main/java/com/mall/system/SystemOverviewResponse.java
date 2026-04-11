package com.mall.system;

import java.util.List;

/**
 * 系统概览接口的响应对象。
 */
public record SystemOverviewResponse(
	String applicationName,
	List<String> modules,
	KafkaView kafka
) {

	/**
	 * Kafka 相关的简化视图。
	 */
	public record KafkaView(
		boolean enabled,
		String bootstrapServers,
		String consumerGroup,
		List<String> topics
	) {
	}
}
