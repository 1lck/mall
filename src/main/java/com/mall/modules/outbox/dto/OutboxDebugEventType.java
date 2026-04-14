package com.mall.modules.outbox.dto;

/**
 * Outbox 调试消息类型。
 *
 * <p>这里不是业务事件类型，而是“要帮你生成哪一种调试场景”。</p>
 */
public enum OutboxDebugEventType {
	/** 生成一条已经发送成功的历史消息。 */
	SENT,
	/** 生成一条等待下次自动重试的失败消息。 */
	FAILED,
	/** 生成一条已经进入死信状态的消息。 */
	DEAD,
	/** 生成一条会在即时投递时稳定失败的消息。 */
	IMMEDIATE_FAIL
}
