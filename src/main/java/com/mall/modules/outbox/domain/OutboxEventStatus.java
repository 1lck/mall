package com.mall.modules.outbox.domain;

/**
 * Outbox 事件发送状态。
 *
 * <p>这里的状态不是业务状态，而是“这条待发消息当前投递到哪一步了”。</p>
 */
public enum OutboxEventStatus {
	// 已经进入 outbox，等待扫描器首次投递。
	PENDING,
	// 已经成功投递到 Kafka，不需要再扫描。
	SENT,
	// 上一次投递失败，等 nextRetryAt 到点后再尝试。
	FAILED,
	// 已达到最大重试次数，不再自动重试，等待人工介入。
	DEAD
}
