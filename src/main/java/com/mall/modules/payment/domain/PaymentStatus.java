package com.mall.modules.payment.domain;

/**
 * 支付记录状态。
 *
 * <p>当前先只落一版最小状态集，
 * 方便后面继续扩展支付成功、支付失败等流程。</p>
 */
public enum PaymentStatus {
	PENDING,
	SUCCESS,
	FAILED;

	/**
	 * 判断当前支付状态是否允许流转到目标状态。
	 */
	public boolean canTransitionTo(PaymentStatus targetStatus) {
		// 同状态重复提交先直接放行，避免重复回调时出现无意义错误。
		if (this == targetStatus) {
			return true;
		}

		return switch (this) {
			case PENDING -> targetStatus == SUCCESS || targetStatus == FAILED;
			case SUCCESS, FAILED -> false;
		};
	}
}
