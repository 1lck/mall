package com.mall.modules.product.vo;

import com.mall.modules.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 返回给前端的商品信息。
 */
public record ProductVO(
	/** 商品主键。 */
	Long id,
	/** 商品编号。 */
	String productNo,
	/** 商品名称。 */
	String name,
	/** 商品类目名称。 */
	String categoryName,
	/** 商品售价。 */
	BigDecimal price,
	/** 当前库存。 */
	Integer stock,
	/** 商品状态。 */
	ProductStatus status,
	/** 商品描述。 */
	String description,
	/** 商品主图地址。 */
	String imageUrl,
	/** 创建时间。 */
	Instant createdAt,
	/** 最近更新时间。 */
	Instant updatedAt
) {
}
