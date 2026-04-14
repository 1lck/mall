package com.mall.modules.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 outbox 调试服务会生成预期的演示数据，并触发一次即时失败演示投递。
 */
class OutboxDebugApplicationServiceTests {

	/**
	 * 生成演示数据时，应保存四条不同状态的记录，并对即时失败那条申请精确投递。
	 */
	@Test
	void shouldCreateDemoBatchAndTriggerImmediateFailEvent() {
		OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
		OutboxDispatchTrigger outboxDispatchTrigger = mock(OutboxDispatchTrigger.class);
		KafkaTopicsProperties kafkaTopicsProperties = new KafkaTopicsProperties();
		kafkaTopicsProperties.getTopics().setPaymentSucceeded("mall.payment.succeeded");
		OutboxDebugApplicationService service = new OutboxDebugApplicationService(
			outboxEventMapper,
			outboxDispatchTrigger,
			kafkaTopicsProperties,
			new ObjectMapper()
		);

		when(outboxEventMapper.save(org.mockito.ArgumentMatchers.any(OutboxEventEntity.class)))
			.thenAnswer(invocation -> {
				OutboxEventEntity entity = invocation.getArgument(0);
				if (entity.getId() == null) {
					entity.setId((long) (entity.getRetryCount() + 1));
				}
				return entity;
			});

		List<OutboxEventAdminVO> result = service.createDemoBatch();

		assertThat(result).hasSize(4);
		assertThat(result).extracting(OutboxEventAdminVO::status)
			.contains(OutboxEventStatus.SENT, OutboxEventStatus.FAILED, OutboxEventStatus.DEAD, OutboxEventStatus.PENDING);

		ArgumentCaptor<OutboxEventEntity> entityCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
		verify(outboxEventMapper, times(4)).save(entityCaptor.capture());
		assertThat(entityCaptor.getAllValues()).extracting(OutboxEventEntity::getEventType)
			.contains("PAYMENT_SUCCEEDED", "DEBUG_UNSUPPORTED_EVENT");
		verify(outboxDispatchTrigger).requestDispatch(1L);
	}
}
