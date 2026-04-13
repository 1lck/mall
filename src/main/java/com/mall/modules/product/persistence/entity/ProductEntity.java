package com.mall.modules.product.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.modules.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 商品表对应的 JPA 实体。
 */
@TableName("products")
public class ProductEntity {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String productNo;

	private String name;

	private String categoryName;

	private BigDecimal price;

	private Integer stock;

	private ProductStatus status;

	private String description;

	private String imageUrl;

	@TableField(fill = FieldFill.INSERT)
	private Instant createdAt;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getProductNo() {
		return productNo;
	}

	public void setProductNo(String productNo) {
		this.productNo = productNo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public ProductStatus getStatus() {
		return status;
	}

	public void setStatus(ProductStatus status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
