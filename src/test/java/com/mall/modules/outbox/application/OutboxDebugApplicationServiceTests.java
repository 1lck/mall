package com.mall.modules.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.dto.OutboxDebugEventType;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
		KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
		KafkaTopicsProperties kafkaTopicsProperties = new KafkaTopicsProperties();
		kafkaTopicsProperties.getTopics().setPaymentSucceeded("mall.payment.succeeded");
		OutboxDebugApplicationService service = new OutboxDebugApplicationService(
			outboxEventMapper,
			outboxDispatchTrigger,
			kafkaTemplate,
			kafkaTopicsProperties,
			new ObjectMapper().findAndRegisterModules()
		);

		when(outboxEventMapper.save(org.mockito.ArgumentMatchers.any(OutboxEventEntity.class)))
			.thenAnswer(invocation -> {
				OutboxEventEntity entity = invocation.getArgument(0);
				if (entity.getId() == null) {
					entity.setId((long) (entity.getRetryCount() + 1));
				}
				return entity;
			});
		when(outboxEventMapper.findById(1L)).thenAnswer(invocation -> {
			OutboxEventEntity entity = new OutboxEventEntity();
			entity.setId(1L);
			entity.setEventId("evt-immediate-fail");
			entity.setAggregateType("PAYMENT");
			entity.setAggregateId("ORD-DEMO-IMMEDIATE-FAIL");
			entity.setEventType("DEBUG_UNSUPPORTED_EVENT");
			entity.setTopic("mall.payment.succeeded");
			entity.setMessageKey("ORD-DEMO-IMMEDIATE-FAIL");
			entity.setStatus(OutboxEventStatus.FAILED);
			entity.setRetryCount(1);
			entity.setLastError("Unsupported outbox event type");
			return Optional.of(entity);
		});

		List<OutboxEventAdminVO> result = service.createDemoBatch();

		assertThat(result).hasSize(4);
		assertThat(result).extracting(OutboxEventAdminVO::status)
			.contains(OutboxEventStatus.SENT, OutboxEventStatus.FAILED, OutboxEventStatus.DEAD);
		assertThat(result).filteredOn(item -> "DEBUG_UNSUPPORTED_EVENT".equals(item.eventType()))
			.singleElement()
			.extracting(OutboxEventAdminVO::status)
			.isEqualTo(OutboxEventStatus.FAILED);

		ArgumentCaptor<OutboxEventEntity> entityCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
		verify(outboxEventMapper, times(4)).save(entityCaptor.capture());
		assertThat(entityCaptor.getAllValues()).extracting(OutboxEventEntity::getEventType)
			.contains("PAYMENT_SUCCEEDED", "DEBUG_UNSUPPORTED_EVENT");
		verify(outboxDispatchTrigger).requestDispatch(1L);
	}

	/**
	 * 生成单条调试消息时，应允许外部指定聚合标识，便于固定断点调试目标。
	 */
	@Test
	void shouldCreateSingleFailedEventWithCustomAggregateId() {
		OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
		OutboxDispatchTrigger outboxDispatchTrigger = mock(OutboxDispatchTrigger.class);
		KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
		KafkaTopicsProperties kafkaTopicsProperties = new KafkaTopicsProperties();
		kafkaTopicsProperties.getTopics().setPaymentSucceeded("mall.payment.succeeded");
		OutboxDebugApplicationService service = new OutboxDebugApplicationService(
			outboxEventMapper,
			outboxDispatchTrigger,
			kafkaTemplate,
			kafkaTopicsProperties,
			new ObjectMapper().findAndRegisterModules()
		);

		when(outboxEventMapper.save(org.mockito.ArgumentMatchers.any(OutboxEventEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		OutboxEventAdminVO result = service.createSingleEvent(OutboxDebugEventType.FAILED, "ORD-DEBUG-CUSTOM-001");

		assertThat(result.aggregateId()).isEqualTo("ORD-DEBUG-CUSTOM-001");
		assertThat(result.status()).isEqualTo(OutboxEventStatus.FAILED);
		assertThat(result.lastError()).isEqualTo("模拟 Kafka 不可用，等待下次自动重试");
	}

	/**
	 * 清理调试数据时，应只调用专门的调试数据删除语句。
	 */
	@Test
	void shouldCleanupDebugEvents() {
		OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
		OutboxDispatchTrigger outboxDispatchTrigger = mock(OutboxDispatchTrigger.class);
		KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
		KafkaTopicsProperties kafkaTopicsProperties = new KafkaTopicsProperties();
		kafkaTopicsProperties.getTopics().setPaymentSucceeded("mall.payment.succeeded");
		OutboxDebugApplicationService service = new OutboxDebugApplicationService(
			outboxEventMapper,
			outboxDispatchTrigger,
			kafkaTemplate,
			kafkaTopicsProperties,
			new ObjectMapper().findAndRegisterModules()
		);

		when(outboxEventMapper.deleteDebugEvents("PAYMENT_DEBUG", "ORD-DEMO-%")).thenReturn(6);

		int deletedCount = service.cleanupDebugEvents();

		assertThat(deletedCount).isEqualTo(6);
		verify(outboxEventMapper).deleteDebugEvents("PAYMENT_DEBUG", "ORD-DEMO-%");
	}

	/**
	 * 直接发送支付成功调试消息时，应等待 Kafka 发送完成后再返回事件内容。
	 */
	@Test
	void shouldSendPaymentSucceededMessageDirectlyToKafka() {
		OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
		OutboxDispatchTrigger outboxDispatchTrigger = mock(OutboxDispatchTrigger.class);
		@SuppressWarnings("unchecked")
		KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
		KafkaTopicsProperties kafkaTopicsProperties = new KafkaTopicsProperties();
		kafkaTopicsProperties.getTopics().setPaymentSucceeded("mall.payment.succeeded");
		OutboxDebugApplicationService service = new OutboxDebugApplicationService(
			outboxEventMapper,
			outboxDispatchTrigger,
			kafkaTemplate,
			kafkaTopicsProperties,
			new ObjectMapper().findAndRegisterModules()
		);
		CompletableFuture<SendResult<String, Object>> sendFuture = CompletableFuture.completedFuture(null);
		when(kafkaTemplate.send(
			org.mockito.ArgumentMatchers.eq("mall.payment.succeeded"),
			org.mockito.ArgumentMatchers.eq("ORD-CONSUMER-FAIL-001"),
			org.mockito.ArgumentMatchers.any(PaymentSucceededEvent.class)
		)).thenReturn(sendFuture);

		PaymentSucceededEvent result = service.sendPaymentSucceededMessage(
			"ORD-CONSUMER-FAIL-001",
			new BigDecimal("88.80")
		);

		ArgumentCaptor<PaymentSucceededEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSucceededEvent.class);
		verify(kafkaTemplate).send(
			org.mockito.ArgumentMatchers.eq("mall.payment.succeeded"),
			org.mockito.ArgumentMatchers.eq("ORD-CONSUMER-FAIL-001"),
			eventCaptor.capture()
		);
		assertThat(sendFuture).isCompleted();
		assertThat(result.orderNo()).isEqualTo("ORD-CONSUMER-FAIL-001");
		assertThat(result.amount()).isEqualByComparingTo("88.80");
		assertThat(result.paidAt()).isNotNull();
		assertThat(Duration.between(eventCaptor.getValue().paidAt(), result.paidAt()).abs())
			.isLessThan(Duration.ofSeconds(1));
	}
}
