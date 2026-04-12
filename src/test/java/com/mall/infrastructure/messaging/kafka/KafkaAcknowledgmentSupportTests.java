package com.mall.infrastructure.messaging.kafka;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class KafkaAcknowledgmentSupportTests {

	@AfterEach
	void clearTransactionSynchronization() {
		TransactionSynchronizationManager.clear();
	}

	@Test
	void acknowledgeAfterCommitShouldAcknowledgeImmediatelyWhenNoTransactionIsActive() {
		Acknowledgment acknowledgment = mock(Acknowledgment.class);

		// 没有事务时不需要等待提交时机，应该立即 ack。
		KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);

		verify(acknowledgment).acknowledge();
	}

	@Test
	void acknowledgeAfterCommitShouldWaitUntilAfterCommitWhenTransactionIsActive() {
		Acknowledgment acknowledgment = mock(Acknowledgment.class);
		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);

		// 有事务时先注册 afterCommit 回调，此时还不应立即 ack。
		KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);

		verifyNoInteractions(acknowledgment);
		// 手动触发 afterCommit，模拟事务真正提交成功的时刻。
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.afterCommit();
		}
		verify(acknowledgment, times(1)).acknowledge();
	}
}
