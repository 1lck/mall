package com.mall.modules.order.domain;

/**
 * 订单状态枚举。
 */
public enum OrderStatus {
	CREATED,
	PAID,
	CANCELLED;

	/**
	 * 判断当前订单状态是否允许流转到目标状态。
	 */
	public boolean canTransitionTo(OrderStatus targetStatus) {
		// 同状态重复提交直接放行，避免前端回填后更新时报无意义错误。
		if (this == targetStatus) {
			return true;
		}

		return switch (this) {
			case CREATED -> targetStatus == PAID || targetStatus == CANCELLED;
			case PAID, CANCELLED -> false;
		};
	}
}
