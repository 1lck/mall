package com.mall.modules.order.api;

import com.mall.modules.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 返回给前端的订单信息。
 */
public record OrderResponse(
	Long id,
	String orderNo,
	Long userId,
	BigDecimal totalAmount,
	OrderStatus status,
	String remark,
	Instant createdAt,
	Instant updatedAt
) {
}
