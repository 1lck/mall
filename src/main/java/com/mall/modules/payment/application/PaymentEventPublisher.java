package com.mall.modules.payment.application;

import com.mall.modules.payment.event.PaymentSucceededEvent;

/**
 * 支付事件发布出口。
 */
public interface PaymentEventPublisher {

	/**
	 * 发布支付成功事件。
	 */
	void publishPaymentSucceeded(PaymentSucceededEvent event);
}
