package com.mall.system;

import java.util.List;

/**
 * Kafka topic 最近消息响应。
 */
public record AdminKafkaTopicMessagesResponse(
	/** 当前查看的 topic。 */
	String topic,
	/** 当前过滤的分区；为空表示查看全部分区。 */
	Integer partition,
	/** 本次请求的消息上限。 */
	int limit,
	/** 最近消息列表。 */
	List<MessageView> messages
) {

	/**
	 * 单条消息的展示视图。
	 */
	public record MessageView(
		/** topic 名称。 */
		String topic,
		/** 分区编号。 */
		int partition,
		/** offset。 */
		long offset,
		/** ISO-8601 时间戳。 */
		String timestamp,
		/** 消息 key。 */
		String key,
		/** 消息 value。 */
		String value,
		/** value 的展示格式标签。 */
		String valueFormat
	) {
	}
}
