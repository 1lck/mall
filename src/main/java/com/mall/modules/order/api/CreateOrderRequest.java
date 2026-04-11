package com.mall.modules.order.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建订单时的请求体。
 */
public record CreateOrderRequest(
	@NotNull(message = "must not be null")
	@DecimalMin(value = "0.01", message = "must be greater than 0")
	BigDecimal totalAmount,
	@Size(max = 255, message = "length must be less than or equal to 255")
	String remark
) {
}
