package com.mall.modules.outbox.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 创建单条 outbox 调试消息的请求参数。
 *
 * <p>当前先保持最小输入：
 * 1. 指定要生成哪种调试消息类型
 * 2. 可选指定聚合标识，方便你自己造固定订单号做断点调试</p>
 */
public record CreateOutboxDebugEventDTO(
	/** 要生成的调试消息类型。 */
	@NotNull(message = "调试消息类型不能为空")
	OutboxDebugEventType type,
	/** 可选聚合标识，例如订单号。为空时后端自动生成。 */
	String aggregateId
) {
}
