package com.mall.infrastructure.messaging;

/**
 * 领域事件发布器抽象。
 *
 * <p>业务模块依赖这个接口，而不是直接依赖 Kafka 之类的具体实现，
 * 这样后面替换消息中间件时改动会更小。</p>
 */
public interface DomainEventPublisher {

	/**
	 * 发布一个领域事件到基础设施层。
	 */
	void publish(DomainEvent event);
}
