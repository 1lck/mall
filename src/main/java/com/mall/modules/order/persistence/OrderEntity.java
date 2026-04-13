package com.mall.modules.order.persistence;

import com.mall.modules.order.domain.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 订单表对应的 JPA 实体。
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_no", nullable = false, unique = true, length = 64)
	private String orderNo;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "product_id")
	private Long productId;

	@Column(name = "quantity")
	private Integer quantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private OrderStatus status;

	@Column(name = "remark", length = 255)
	private String remark;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		// 首次入库时同时写入创建时间和更新时间。
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		// 每次更新前自动刷新更新时间。
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
