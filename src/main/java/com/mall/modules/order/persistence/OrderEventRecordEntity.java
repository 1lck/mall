package com.mall.modules.order.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 订单事件消费记录。
 *
 * <p>第一版先只记录最关键的信息：
 * 处理了什么事件、对应哪笔订单、处理时间是什么时候。</p>
 *
 * <p>后面如果你要继续做幂等、防重复消费，
 * 可以在这张表上继续补唯一约束、消息 ID、处理结果等字段。</p>
 */
@Entity
@Table(name = "order_event_records")
public class OrderEventRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_type", nullable = false, length = 64)
	private String eventType;

	@Column(name = "order_no", nullable = false, length = 64)
	private String orderNo;

	@Column(name = "processed_at", nullable = false, updatable = false)
	private Instant processedAt;

	@PrePersist
	void onCreate() {
		// 记录这条消费记录首次入库的时间。
		this.processedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}
}
