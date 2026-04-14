package com.mall.modules.payment.application;

/**
 * 支付应用服务接口。
 */
public interface PaymentApplicationService {

	/**
	 * 把指定订单标记为支付成功。
	 */
	void markPaymentSuccess(String orderNo);
}
