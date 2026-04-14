package com.mall.infrastructure.messaging.kafka;

import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Kafka 手动确认辅助方法。
 *
 * <p>优先把 ack 延后到数据库事务真正提交成功之后，
 * 避免“事务回滚了但 offset 已提交”的时序问题。</p>
 */
public final class KafkaAcknowledgmentSupport {

	private KafkaAcknowledgmentSupport() {
	}

	/**
	 * 把 Kafka 手动确认动作延后到事务提交之后执行。
	 */
	public static void acknowledgeAfterCommit(Acknowledgment acknowledgment) {
		// 当前如果正处在 Spring 管理的数据库事务里，
		// 就不要立刻 ack，而是把 ack 这个动作挂到 afterCommit 回调里。
		// 这样只有事务真正提交成功后，Kafka 才会把这条消息标记为“已处理”。
		if (
			TransactionSynchronizationManager.isActualTransactionActive()
				&& TransactionSynchronizationManager.isSynchronizationActive()
		) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					// afterCommit 由 Spring 在事务成功提交后自动调用。
					// 也就是说，能走到这里就说明数据库里的改动已经正式落盘了，
					// 这时再 ack，时序才是安全的。
					acknowledgment.acknowledge();
				}
			});
			return;
		}

		// 如果当前没有事务上下文，就没必要等待提交时机，直接 ack 即可。
		acknowledgment.acknowledge();
	}
}
