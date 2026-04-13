package com.mall.modules.outbox.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Outbox 即时触发器。
 *
 * <p>它不负责真正发送消息，只负责决定“什么时候立即补一枪扫描”：
 * 如果当前正处在业务事务里，就把这次扫描挂到 afterCommit；
 * 如果当前没有事务，直接触发一次 dispatch。</p>
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class OutboxDispatchTrigger {

	private final OutboxEventDispatcher outboxEventDispatcher;

	public OutboxDispatchTrigger(OutboxEventDispatcher outboxEventDispatcher) {
		this.outboxEventDispatcher = outboxEventDispatcher;
	}

	public void requestDispatch(Long outboxEventId) {
		// 如果当前还在业务事务里，不能立刻扫描并投递，
		// 否则可能在 outbox 记录还没真正提交时就开始查库。
		// 所以这里把“精确投递这条刚写入的 outbox 记录”挂到 afterCommit，
		// 等事务成功提交后再触发。
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					outboxEventDispatcher.dispatchEventById(outboxEventId);
				}
			});
			return;
		}

		// 没有事务时说明当前不需要等待提交，直接精确投递当前这条记录即可。
		outboxEventDispatcher.dispatchEventById(outboxEventId);
	}
}
