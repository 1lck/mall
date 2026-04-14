package com.mall.modules.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建订单时的请求体。
 */
public record CreateOrderDTO(
	/** 下单商品 id。 */
	@NotNull(message = "must not be null")
	@Min(value = 1, message = "must be greater than or equal to 1")
	Long productId,
	/** 下单数量。 */
	@NotNull(message = "must not be null")
	@Min(value = 1, message = "must be greater than or equal to 1")
	Integer quantity,
	/** 买家备注。 */
	@Size(max = 255, message = "length must be less than or equal to 255")
	String remark
) {
}
