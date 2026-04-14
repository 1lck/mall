package com.mall.modules.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
		OutboxAdminApplicationService service = new OutboxAdminApplicationService(outboxEventMapper);

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
		OutboxAdminApplicationService service = new OutboxAdminApplicationService(outboxEventMapper);

		when(outboxEventMapper.findAdminList(null, null, 500)).thenReturn(List.of());

		List<OutboxEventAdminVO> result = service.listEvents(null, null, 9999);

		assertThat(result).isEmpty();
		verify(outboxEventMapper).findAdminList(null, null, 500);
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
