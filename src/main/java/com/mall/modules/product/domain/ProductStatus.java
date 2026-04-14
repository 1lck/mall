package com.mall.modules.product.domain;

/**
 * 商品状态枚举。
 */
public enum ProductStatus {
	/** 草稿状态，尚未对外销售。 */
	DRAFT,
	/** 在售状态。 */
	ON_SALE,
	/** 已下架状态。 */
	OFF_SHELF;

	/**
	 * 判断当前商品状态是否允许流转到目标状态。
	 */
	public boolean canTransitionTo(ProductStatus targetStatus) {
		// 同状态重复提交直接放行，避免编辑页保存时被误判成非法流转。
		if (this == targetStatus) {
			return true;
		}

		return switch (this) {
			case DRAFT -> targetStatus == ON_SALE;
			case ON_SALE -> targetStatus == OFF_SHELF;
			case OFF_SHELF -> targetStatus == ON_SALE;
		};
	}
}
