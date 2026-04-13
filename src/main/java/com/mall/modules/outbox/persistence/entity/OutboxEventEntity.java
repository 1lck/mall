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
 */
@TableName(value = "outbox_events", autoResultMap = true)
public class OutboxEventEntity {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String eventId;

	private String aggregateType;

	private String aggregateId;

	private String eventType;

	private String topic;

	private String messageKey;

	@TableField(typeHandler = JacksonTypeHandler.class)
	private JsonNode payload;

	private OutboxEventStatus status;

	private Integer retryCount;

	private Instant nextRetryAt;

	private String lastError;

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
