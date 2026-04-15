package com.mall.modules.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 outbox 后台查询应用服务的查询参数处理和 VO 转换逻辑。
 */
class OutboxAdminApplicationServiceTests {

	/**
	 * 当 limit 为空或非法时，应用服务会回退到默认条数并正确转换结果。
	 */
	@Test
	void shouldUseDefaultLimitAndMapEntityToAdminVO() {
		OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
		OutboxDispatchTrigger outboxDispatchTrigger = mock(OutboxDispatchTrigger.class);
		OutboxAdminApplicationService service = new OutboxAdminApplicationService(outboxEventMapper, outboxDispatchTrigger);

		OutboxEventEntity entity = buildEntity();
		when(outboxEventMapper.findAdminList(OutboxEventStatus.FAILED, "ORDER-001", 200))
			.thenReturn(List.of(entity));

		List<OutboxEventAdminVO> result = service.listEvents(OutboxEventStatus.FAILED, "ORDER-001", null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).eventId()).isEqualTo("evt-001");
		assertThat(result.get(0).aggregateId()).isEqualTo("ORDER-001");
		assertThat(result.get(0).status()).isEqualTo(OutboxEventStatus.FAILED);
		assertThat(result.get(0).retryCount()).isEqualTo(2);
		verify(outboxEventMapper).findAdminList(OutboxEventStatus.FAILED, "ORDER-001", 200);
	}

	/**
	 * 当 limit 超过上限时，应用服务会自动截断，避免一次查询太多记录。
	 */
	@Test
	void shouldCapLimitToMaximumValue() {
		OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
		OutboxDispatchTrigger outboxDispatchTrigger = mock(OutboxDispatchTrigger.class);
		OutboxAdminApplicationService service = new OutboxAdminApplicationService(outboxEventMapper, outboxDispatchTrigger);

		when(outboxEventMapper.findAdminList(null, null, 500)).thenReturn(List.of());

		List<OutboxEventAdminVO> result = service.listEvents(null, null, 9999);

		assertThat(result).isEmpty();
		verify(outboxEventMapper).findAdminList(null, null, 500);
	}

	/**
	 * 手动重发时，应先重置状态，再立即触发一次精确投递。
	 */
	@Test
	void shouldResetAndDispatchRetryableOutboxEvent() {
		OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
		OutboxDispatchTrigger outboxDispatchTrigger = mock(OutboxDispatchTrigger.class);
		OutboxAdminApplicationService service = new OutboxAdminApplicationService(outboxEventMapper, outboxDispatchTrigger);

		OutboxEventEntity failedEntity = buildEntity();
		failedEntity.setStatus(OutboxEventStatus.FAILED);
		OutboxEventEntity pendingEntity = buildEntity();
		pendingEntity.setStatus(OutboxEventStatus.PENDING);
		pendingEntity.setRetryCount(0);
		pendingEntity.setLastError(null);
		pendingEntity.setNextRetryAt(null);
		when(outboxEventMapper.findById(1L)).thenReturn(java.util.Optional.of(failedEntity), java.util.Optional.of(pendingEntity));

		OutboxEventAdminVO result = service.retryEvent(1L);

		assertThat(result.status()).isEqualTo(OutboxEventStatus.PENDING);
		verify(outboxEventMapper).resetForManualRetry(1L);
		verify(outboxDispatchTrigger).requestDispatch(1L);
	}

	/**
	 * 非 FAILED/DEAD 状态的消息，不允许人工重发。
	 */
	@Test
	void shouldRejectRetryForNonRetryableStatus() {
		OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
		OutboxDispatchTrigger outboxDispatchTrigger = mock(OutboxDispatchTrigger.class);
		OutboxAdminApplicationService service = new OutboxAdminApplicationService(outboxEventMapper, outboxDispatchTrigger);

		OutboxEventEntity sentEntity = buildEntity();
		sentEntity.setStatus(OutboxEventStatus.SENT);
		when(outboxEventMapper.findById(1L)).thenReturn(java.util.Optional.of(sentEntity));

		assertThatThrownBy(() -> service.retryEvent(1L))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Only FAILED or DEAD outbox events can be retried manually.");
		verify(outboxEventMapper, never()).resetForManualRetry(1L);
		verify(outboxDispatchTrigger, never()).requestDispatch(1L);
	}

	/**
	 * 构造一条典型的失败 outbox 记录，供查询映射测试复用。
	 */
	private OutboxEventEntity buildEntity() {
		ObjectMapper objectMapper = new ObjectMapper();
		OutboxEventEntity entity = new OutboxEventEntity();
		entity.setId(1L);
		entity.setEventId("evt-001");
		entity.setAggregateType("PAYMENT");
		entity.setAggregateId("ORDER-001");
		entity.setEventType("PAYMENT_SUCCEEDED");
		entity.setTopic("mall.payment.succeeded");
		entity.setMessageKey("ORDER-001");
		entity.setPayload(objectMapper.createObjectNode().put("orderNo", "ORDER-001"));
		entity.setStatus(OutboxEventStatus.FAILED);
		entity.setRetryCount(2);
		entity.setNextRetryAt(Instant.parse("2026-04-14T12:00:00Z"));
		entity.setLastError("broker unavailable");
		entity.setSentAt(null);
		ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-04-14T11:58:00Z"));
		ReflectionTestUtils.setField(entity, "updatedAt", Instant.parse("2026-04-14T11:59:00Z"));
		return entity;
	}
}
