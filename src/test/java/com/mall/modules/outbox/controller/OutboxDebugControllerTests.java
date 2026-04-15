package com.mall.modules.outbox.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.outbox.application.OutboxDebugApplicationService;
import com.mall.modules.outbox.dto.CreateOutboxDebugEventDTO;
import com.mall.modules.outbox.dto.OutboxDebugEventType;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 outbox 调试控制器的响应结构。
 */
class OutboxDebugControllerTests {

	/**
	 * 控制器应返回 201，并把服务生成的数据透传给前端。
	 */
	@Test
	void shouldReturnCreatedDemoBatch() {
		OutboxDebugApplicationService outboxDebugApplicationService = mock(OutboxDebugApplicationService.class);
		OutboxDebugController controller = new OutboxDebugController(outboxDebugApplicationService);

		OutboxEventAdminVO event = new OutboxEventAdminVO(
			1L,
			"evt-demo-001",
			"PAYMENT",
			"ORD-DEMO-001",
			"PAYMENT_SUCCEEDED",
			"mall.payment.succeeded",
			"ORD-DEMO-001",
			OutboxEventStatus.SENT,
			0,
			null,
			null,
			Instant.parse("2026-04-14T13:00:00Z"),
			Instant.parse("2026-04-14T12:59:00Z"),
			Instant.parse("2026-04-14T13:00:00Z")
		);
		when(outboxDebugApplicationService.createDemoBatch()).thenReturn(List.of(event));

		ResponseEntity<ApiResponse<List<OutboxEventAdminVO>>> response = controller.createDemoBatch();

		assertThat(response.getStatusCode().value()).isEqualTo(201);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().data()).hasSize(1);
		verify(outboxDebugApplicationService).createDemoBatch();
	}

	/**
	 * 控制器应支持按类型创建单条调试消息。
	 */
	@Test
	void shouldReturnCreatedSingleDebugEvent() {
		OutboxDebugApplicationService outboxDebugApplicationService = mock(OutboxDebugApplicationService.class);
		OutboxDebugController controller = new OutboxDebugController(outboxDebugApplicationService);

		OutboxEventAdminVO event = new OutboxEventAdminVO(
			2L,
			"evt-debug-single",
			"PAYMENT",
			"ORD-DEBUG-SINGLE-001",
			"PAYMENT_SUCCEEDED",
			"mall.payment.succeeded",
			"ORD-DEBUG-SINGLE-001",
			OutboxEventStatus.FAILED,
			1,
			Instant.parse("2026-04-14T13:10:00Z"),
			"模拟 Kafka 不可用，等待下次自动重试",
			null,
			Instant.parse("2026-04-14T13:00:00Z"),
			Instant.parse("2026-04-14T13:00:00Z")
		);
		when(outboxDebugApplicationService.createSingleEvent(OutboxDebugEventType.FAILED, "ORD-DEBUG-SINGLE-001"))
			.thenReturn(event);

		ResponseEntity<ApiResponse<OutboxEventAdminVO>> response = controller.createSingleEvent(
			new CreateOutboxDebugEventDTO(OutboxDebugEventType.FAILED, "ORD-DEBUG-SINGLE-001")
		);

		assertThat(response.getStatusCode().value()).isEqualTo(201);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().data().aggregateId()).isEqualTo("ORD-DEBUG-SINGLE-001");
		verify(outboxDebugApplicationService).createSingleEvent(OutboxDebugEventType.FAILED, "ORD-DEBUG-SINGLE-001");
	}

	/**
	 * 控制器应支持清理旧调试数据。
	 */
	@Test
	void shouldCleanupDebugEvents() {
		OutboxDebugApplicationService outboxDebugApplicationService = mock(OutboxDebugApplicationService.class);
		OutboxDebugController controller = new OutboxDebugController(outboxDebugApplicationService);

		when(outboxDebugApplicationService.cleanupDebugEvents()).thenReturn(8);

		ApiResponse<Integer> response = controller.cleanupDebugEvents();

		assertThat(response.success()).isTrue();
		assertThat(response.data()).isEqualTo(8);
		verify(outboxDebugApplicationService).cleanupDebugEvents();
	}
}
