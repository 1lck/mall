package com.mall.modules.outbox.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxDispatchTriggerTests {

	@Mock
	private OutboxEventDispatcher outboxEventDispatcher;

	@Test
	void requestDispatchShouldRunImmediatelyWhenNoTransactionExists() {
		OutboxDispatchTrigger trigger = new OutboxDispatchTrigger(outboxEventDispatcher);

		trigger.requestDispatch(42L);

		verify(outboxEventDispatcher).dispatchEventById(42L);
	}

	@Test
	void requestDispatchShouldDeferUntilAfterCommitWhenTransactionActive() {
		OutboxDispatchTrigger trigger = new OutboxDispatchTrigger(outboxEventDispatcher);

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		try {
			trigger.requestDispatch(42L);

			verify(outboxEventDispatcher, never()).dispatchEventById(42L);

			List<TransactionSynchronization> synchronizations =
				TransactionSynchronizationManager.getSynchronizations();
			for (TransactionSynchronization synchronization : synchronizations) {
				synchronization.afterCommit();
			}

			verify(outboxEventDispatcher).dispatchEventById(42L);
		} finally {
			TransactionSynchronizationManager.setActualTransactionActive(false);
			TransactionSynchronizationManager.clearSynchronization();
		}
	}
}
