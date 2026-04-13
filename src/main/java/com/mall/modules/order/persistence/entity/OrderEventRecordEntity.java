package com.mall.modules.order.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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
@TableName("order_event_records")
public class OrderEventRecordEntity {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String eventType;

	private String orderNo;

	@TableField(fill = FieldFill.INSERT)
	private Instant processedAt;

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
