package com.mall.modules.order.vo;

import com.mall.modules.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 返回给前端的订单信息。
 */
public record OrderVO(
	/** 订单主键。 */
	Long id,
	/** 订单编号。 */
	String orderNo,
	/** 下单用户 id。 */
	Long userId,
	/** 订单总金额。 */
	BigDecimal totalAmount,
	/** 商品 id。 */
	Long productId,
	/** 商品购买数量。 */
	Integer quantity,
	/** 订单状态。 */
	OrderStatus status,
	/** 订单备注。 */
	String remark,
	/** 创建时间。 */
	Instant createdAt,
	/** 最近更新时间。 */
	Instant updatedAt
) {
}
