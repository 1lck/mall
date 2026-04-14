package com.mall.modules.outbox.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.outbox.application.OutboxAdminApplicationService;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 outbox 后台控制器会按原样透传查询条件并返回统一响应结构。
 */
class OutboxAdminControllerTests {

	/**
	 * 控制器应把查询参数交给应用服务，并返回成功响应。
	 */
	@Test
	void shouldReturnAdminOutboxList() {
		OutboxAdminApplicationService outboxAdminApplicationService = mock(OutboxAdminApplicationService.class);
		OutboxAdminController controller = new OutboxAdminController(outboxAdminApplicationService);

		OutboxEventAdminVO event = new OutboxEventAdminVO(
			1L,
			"evt-001",
			"PAYMENT",
			"ORDER-001",
			"PAYMENT_SUCCEEDED",
			"mall.payment.succeeded",
			"ORDER-001",
			OutboxEventStatus.FAILED,
			2,
			Instant.parse("2026-04-14T12:00:00Z"),
			"broker unavailable",
			null,
			Instant.parse("2026-04-14T11:58:00Z"),
			Instant.parse("2026-04-14T11:59:00Z")
		);
		when(outboxAdminApplicationService.listEvents(OutboxEventStatus.FAILED, "ORDER-001", 50))
			.thenReturn(List.of(event));

		ApiResponse<List<OutboxEventAdminVO>> response =
			controller.listOutboxEvents(OutboxEventStatus.FAILED, "ORDER-001", 50);

		assertThat(response.success()).isTrue();
		assertThat(response.data()).hasSize(1);
		assertThat(response.data().get(0).eventId()).isEqualTo("evt-001");
		verify(outboxAdminApplicationService).listEvents(OutboxEventStatus.FAILED, "ORDER-001", 50);
	}
}
