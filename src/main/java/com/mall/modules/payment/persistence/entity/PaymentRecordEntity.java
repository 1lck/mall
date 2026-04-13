package com.mall.modules.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.modules.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 支付记录表对应的 JPA 实体。
 *
 * <p>这一版先只保留最关键的信息：
 * 哪笔订单、支付金额、当前支付状态。</p>
 */
@TableName("payment_records")
public class PaymentRecordEntity {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String orderNo;

	private BigDecimal amount;

	private PaymentStatus status;

	@TableField(fill = FieldFill.INSERT)
	private Instant createdAt;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
