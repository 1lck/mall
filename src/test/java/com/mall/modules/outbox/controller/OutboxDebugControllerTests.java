package com.mall.modules.outbox.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.outbox.application.OutboxDebugApplicationService;
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
}
