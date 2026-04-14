package com.mall.modules.payment.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 支付成功事件。
 */
public record PaymentSucceededEvent(
	/** 支付成功的订单编号。 */
	String orderNo,
	/** 实际支付金额。 */
	BigDecimal amount,
	/** 支付完成时间。 */
	Instant paidAt
) {
}
