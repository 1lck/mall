package com.mall.modules.payment.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 支付成功事件。
 */
public record PaymentSucceededEvent(
	String orderNo,
	BigDecimal amount,
	Instant paidAt
) {
}
