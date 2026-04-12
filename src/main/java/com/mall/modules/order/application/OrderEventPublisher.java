package com.mall.modules.order.application;

import com.mall.modules.order.event.OrderCreatedEvent;

/**
 * 订单事件发布出口。
 *
 * <p>这层抽象的意义很重要：
 * 业务层只表达“我要发布订单事件”，
 * 但不直接关心底层到底是 Kafka、RocketMQ，还是一个本地空实现。</p>
 */
public interface OrderEventPublisher {

	/**
	 * 发布“订单创建成功”事件。
	 */
	void publishOrderCreated(OrderCreatedEvent event);
}
