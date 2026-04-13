package com.mall.modules.outbox.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.mall.modules.outbox.domain.OutboxEventStatus;

import java.time.Instant;

/**
 * 通用 outbox 事件表实体。
 *
 * <p>业务事务里不再直接把消息发到 Kafka，
 * 而是先把“待发送事件”完整落到这张表里。
 * 后面的扫描投递器再统一读取这些记录并真正发消息。</p>
 */
@TableName(value = "outbox_events", autoResultMap = true)
public class OutboxEventEntity {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String eventId;

	// 标识这条消息属于哪个业务聚合，方便后面按领域排查问题。
	private String aggregateType;

	// 聚合实例标识。当前支付链路里直接使用 orderNo。
	private String aggregateId;

	// 事件类型决定扫描器该把 payload 还原成哪种消息对象。
	private String eventType;

	// 真正投递时发往哪个 Kafka topic。
	private String topic;

	// Kafka message key，通常按订单号等业务键分区。
	private String messageKey;

	// 用 jsonb 保存完整事件体，保证“待发消息”先被可靠落库。
	@TableField(typeHandler = JacksonTypeHandler.class)
	private JsonNode payload;

	// 当前 outbox 投递状态。
	private OutboxEventStatus status;

	// 已经尝试投递过多少次，用来做退避和问题排查。
	private Integer retryCount;

	// 下一次允许重试的时间点。
	private Instant nextRetryAt;

	// 最近一次失败原因，便于直接从表里看出为什么没发出去。
	private String lastError;

	// 成功发到 Kafka 的时间。
	private Instant sentAt;

	@TableField(fill = FieldFill.INSERT)
	private Instant createdAt;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public void setAggregateType(String aggregateType) {
		this.aggregateType = aggregateType;
	}

	public String getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(String aggregateId) {
		this.aggregateId = aggregateId;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}

	public JsonNode getPayload() {
		return payload;
	}

	public void setPayload(JsonNode payload) {
		this.payload = payload;
	}

	public OutboxEventStatus getStatus() {
		return status;
	}

	public void setStatus(OutboxEventStatus status) {
		this.status = status;
	}

	public Integer getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(Integer retryCount) {
		this.retryCount = retryCount;
	}

	public Instant getNextRetryAt() {
		return nextRetryAt;
	}

	public void setNextRetryAt(Instant nextRetryAt) {
		this.nextRetryAt = nextRetryAt;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public void setSentAt(Instant sentAt) {
		this.sentAt = sentAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
