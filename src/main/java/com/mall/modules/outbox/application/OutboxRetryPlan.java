package com.mall.modules.outbox.application;

import com.mall.modules.outbox.domain.OutboxEventStatus;

import java.time.Instant;

/**
 * 一次发送失败后的后续处理计划。
 *
 * <p>扫描器不自己拼 if/else 来决定“是继续重试，还是进入 DEAD”，
 * 而是统一交给这个结果对象表达：
 * 当前应该落成什么状态、重试次数是多少、下一次什么时候再试。</p>
 */
public record OutboxRetryPlan(
	OutboxEventStatus status,
	Integer retryCount,
	Instant nextRetryAt,
	String lastError
) {
}
