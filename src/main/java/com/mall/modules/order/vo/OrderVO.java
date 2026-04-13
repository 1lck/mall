package com.mall.modules.order.vo;

import com.mall.modules.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 返回给前端的订单信息。
 */
public record OrderVO(
	Long id,
	String orderNo,
	Long userId,
	BigDecimal totalAmount,
	Long productId,
	Integer quantity,
	OrderStatus status,
	String remark,
	Instant createdAt,
	Instant updatedAt
) {
}
