package com.mall.modules.payment.application;

/**
 * 支付应用服务接口。
 */
public interface PaymentApplicationService {

	void markPaymentSuccess(String orderNo);
}
