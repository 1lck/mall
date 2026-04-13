package com.mall.modules.outbox.domain;

/**
 * Outbox 事件发送状态。
 */
public enum OutboxEventStatus {
	PENDING,
	SENT,
	FAILED
}
