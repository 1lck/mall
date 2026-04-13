package com.mall.modules.product.vo;

import com.mall.modules.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 返回给前端的商品信息。
 */
public record ProductVO(
	Long id,
	String productNo,
	String name,
	String categoryName,
	BigDecimal price,
	Integer stock,
	ProductStatus status,
	String description,
	String imageUrl,
	Instant createdAt,
	Instant updatedAt
) {
}
