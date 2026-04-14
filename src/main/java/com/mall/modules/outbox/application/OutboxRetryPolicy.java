package com.mall.modules.outbox.application;

import com.mall.modules.outbox.domain.OutboxEventStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Outbox 重试策略。
 *
 * <p>第一版先把策略收口在一个地方：
 * 1. 最多自动重试 4 次
 * 2. 重试间隔按 10s -> 30s -> 60s -> 300s 退避
 * 3. 超过最大次数后进入 DEAD，停止自动重试</p>
 */
@Component
public class OutboxRetryPolicy {

	/** 自动重试的最大次数。 */
	private static final int MAX_RETRY_COUNT = 4;
	/** 每次失败后的退避时间，单位为秒。 */
	private static final List<Long> RETRY_BACKOFF_SECONDS = List.of(10L, 30L, 60L, 300L);

	/**
	 * 根据当前失败次数生成下一次重试计划。
	 */
	public OutboxRetryPlan planFailure(Integer currentRetryCount, Instant now, String lastError) {
		// currentRetryCount 表示“失败前已经重试过几次”。
		// 所以这次失败一旦发生，就要先把次数 +1，得到新的失败次数。
		int nextRetryCount = currentRetryCount == null ? 1 : currentRetryCount + 1;

		// 如果新的失败次数已经超过最大自动重试上限，
		// 这条消息就不再继续自动重试，而是进入 DEAD，等待人工处理。
		if (nextRetryCount > MAX_RETRY_COUNT) {
			return new OutboxRetryPlan(
				OutboxEventStatus.DEAD,
				nextRetryCount,
				null,
				lastError
			);
		}

		// 还没超过上限时，继续保留在 FAILED，
		// 并按“第几次失败”取出对应的退避时间。
		// 例如：
		// 第 1 次失败 -> 10 秒后再试
		// 第 2 次失败 -> 30 秒后再试
		// 第 3 次失败 -> 60 秒后再试
		// 第 4 次失败 -> 300 秒后再试
		long backoffSeconds = RETRY_BACKOFF_SECONDS.get(nextRetryCount - 1);

		// 这里返回的不是数据库实体，而是“失败后应该怎么处理”的计划：
		// - 状态保持 FAILED
		// - 重试次数更新为 nextRetryCount
		// - nextRetryAt 设为 now + backoffSeconds
		// - lastError 记录本次失败原因
		return new OutboxRetryPlan(
			OutboxEventStatus.FAILED,
			nextRetryCount,
			now.plusSeconds(backoffSeconds),
			lastError
		);
	}
}
