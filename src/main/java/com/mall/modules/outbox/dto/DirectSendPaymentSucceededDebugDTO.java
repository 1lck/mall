package com.mall.modules.outbox.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 直接发送支付成功 Kafka 调试消息的请求参数。
 *
 * <p>这个接口不经过 outbox，专门用于练习消费者链路，
 * 比如手动制造“消费者第一次失败、第二次重投成功”的场景。</p>
 */
public record DirectSendPaymentSucceededDebugDTO(
	/** 要发送到 Kafka 的订单号。 */
	@NotBlank(message = "订单号不能为空")
	String orderNo,
	/** 可选支付金额；为空时后端会自动补默认值。 */
	@DecimalMin(value = "0.01", message = "支付金额必须大于 0")
	BigDecimal amount
) {
}
