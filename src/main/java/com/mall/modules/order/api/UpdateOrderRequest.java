package com.mall.modules.order.api;

import com.mall.modules.order.domain.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 更新订单时允许修改的字段。
 */
public record UpdateOrderRequest(
	@NotNull(message = "must not be null")
	@DecimalMin(value = "0.01", message = "must be greater than 0")
	BigDecimal totalAmount,
	@NotNull(message = "must not be null")
	OrderStatus status,
	@Size(max = 255, message = "length must be less than or equal to 255")
	String remark
) {
}
