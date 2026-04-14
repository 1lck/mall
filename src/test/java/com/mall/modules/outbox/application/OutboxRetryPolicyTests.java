package com.mall.modules.outbox.application;

import com.mall.modules.outbox.domain.OutboxEventStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRetryPolicyTests {

	private final OutboxRetryPolicy outboxRetryPolicy = new OutboxRetryPolicy();

	@Test
	void planFailureShouldApplyExponentialBackoffInsideRetryLimit() {
		Instant now = Instant.parse("2026-04-14T09:00:00Z");

		OutboxRetryPlan retryPlan = outboxRetryPolicy.planFailure(2, now, "temporary broker error");

		assertThat(retryPlan.status()).isEqualTo(OutboxEventStatus.FAILED);
		assertThat(retryPlan.retryCount()).isEqualTo(3);
		assertThat(retryPlan.nextRetryAt()).isEqualTo(now.plusSeconds(60));
		assertThat(retryPlan.lastError()).isEqualTo("temporary broker error");
	}

	@Test
	void planFailureShouldMoveToDeadWhenRetryLimitIsExceeded() {
		Instant now = Instant.parse("2026-04-14T09:00:00Z");

		OutboxRetryPlan retryPlan = outboxRetryPolicy.planFailure(4, now, "still failing");

		assertThat(retryPlan.status()).isEqualTo(OutboxEventStatus.DEAD);
		assertThat(retryPlan.retryCount()).isEqualTo(5);
		assertThat(retryPlan.nextRetryAt()).isNull();
		assertThat(retryPlan.lastError()).isEqualTo("still failing");
	}
}
