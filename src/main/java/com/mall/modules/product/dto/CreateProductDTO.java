package com.mall.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建商品时的请求体。
 */
public record CreateProductDTO(
	@NotBlank(message = "must not be blank")
	@Size(max = 120, message = "length must be less than or equal to 120")
	String name,
	@NotBlank(message = "must not be blank")
	@Size(max = 100, message = "length must be less than or equal to 100")
	String categoryName,
	@NotNull(message = "must not be null")
	@DecimalMin(value = "0.01", message = "must be greater than 0")
	BigDecimal price,
	@NotNull(message = "must not be null")
	@Min(value = 0, message = "must be greater than or equal to 0")
	Integer stock,
	@Size(max = 500, message = "length must be less than or equal to 500")
	String description,
	@Size(max = 500, message = "length must be less than or equal to 500")
	String imageUrl
) {
}
