package com.mall.modules.order.event;

import com.mall.infrastructure.messaging.DomainEvent;

import java.time.Instant;

/**
 * 订单创建事件。
 */
public record OrderCreatedEvent(Long orderId, Long userId, Instant occurredAt) implements DomainEvent {

	@Override
	public String type() {
		// 事件类型字符串需要稳定，后续消息路由和反序列化都会依赖它。
		return "order.created";
	}
}
