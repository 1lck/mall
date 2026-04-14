package com.mall.modules.outbox.vo;

import com.mall.modules.outbox.domain.OutboxEventStatus;

import java.time.Instant;

/**
 * 后台 outbox 观察页使用的视图对象。
 *
 * <p>这层不关心 payload 的完整消息体，
 * 只返回排查投递状态最常用的字段，方便在管理端快速看出：
 * 这条消息是什么、现在投递到哪一步、失败了几次、下次何时重试。</p>
 */
public record OutboxEventAdminVO(
	/** outbox 表主键。 */
	Long id,
	/** 消息唯一业务 id，用来区分每一条待发事件。 */
	String eventId,
	/** 聚合类型，例如 PAYMENT。 */
	String aggregateType,
	/** 聚合实例 id，例如订单号。 */
	String aggregateId,
	/** 事件类型，例如 PAYMENT_SUCCEEDED。 */
	String eventType,
	/** Kafka topic 名称。 */
	String topic,
	/** Kafka message key。 */
	String messageKey,
	/** 当前 outbox 投递状态。 */
	OutboxEventStatus status,
	/** 当前已经重试过多少次。 */
	Integer retryCount,
	/** 下一次允许自动重试的时间。 */
	Instant nextRetryAt,
	/** 最近一次失败原因。 */
	String lastError,
	/** 真正成功发往 Kafka 的时间。 */
	Instant sentAt,
	/** 记录创建时间。 */
	Instant createdAt,
	/** 最近一次更新时间。 */
	Instant updatedAt
) {
}
