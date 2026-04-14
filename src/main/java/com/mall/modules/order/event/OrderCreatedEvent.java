package com.mall.modules.order.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 订单创建成功后发出的领域事件。
 *
 * <p>你可以把它理解成：
 * “订单已经创建好了，请把这件事通知给异步世界。”</p>
 *
 * <p>第一版先只带最关键的几个字段，
 * 够我们验证下单 -> 发消息 -> 消费消息这条链路。</p>
 */
public record OrderCreatedEvent(
	/** 订单主键。 */
	Long orderId,
	/** 订单编号。 */
	String orderNo,
	/** 下单用户 id。 */
	Long userId,
	/** 订单总金额。 */
	BigDecimal totalAmount,
	/** 订单状态字符串。 */
	String status,
	/** 订单创建时间。 */
	Instant createdAt
) {
}
