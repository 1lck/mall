package com.mall.infrastructure.messaging;

/**
 * 领域事件标记接口。
 *
 * <p>像 {@code OrderCreatedEvent} 这样的具体事件都会实现它，
 * 这样基础设施层就可以用统一方式发布事件。</p>
 */
public interface DomainEvent {

	String type();
}
