package com.mall.modules.order.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建订单时的请求体。
 */
public record CreateOrderRequest(
	@NotNull(message = "must not be null")
	@Min(value = 1, message = "must be greater than or equal to 1")
	Long productId,
	@NotNull(message = "must not be null")
	@Min(value = 1, message = "must be greater than or equal to 1")
	Integer quantity,
	@Size(max = 255, message = "length must be less than or equal to 255")
	String remark
) {
}
