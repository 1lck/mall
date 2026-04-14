package com.mall.modules.order.domain;

/**
 * 订单状态枚举。
 */
public enum OrderStatus {
	/** 已创建，等待支付。 */
	CREATED,
	/** 已支付，进入后续履约流程。 */
	PAID,
	/** 已取消，不再继续处理。 */
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
