package com.mall.modules.outbox.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.outbox.application.OutboxDebugApplicationService;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Outbox 调试控制器。
 *
 * <p>这个控制器只在本地显式开启调试开关时暴露，
 * 用于快速生成演示数据，避免每次都手改数据库。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/outbox-debug")
@Tag(name = "Admin Outbox Debug", description = "Admin outbox debug endpoints")
@ConditionalOnProperty(name = {"mall.outbox.debug.enabled", "mall.kafka.enabled"}, havingValue = "true")
public class OutboxDebugController {

	private final OutboxDebugApplicationService outboxDebugApplicationService;

	public OutboxDebugController(OutboxDebugApplicationService outboxDebugApplicationService) {
		this.outboxDebugApplicationService = outboxDebugApplicationService;
	}

	/**
	 * 一次生成一组 outbox 演示数据。
	 *
	 * @return 刚创建的演示数据列表，前端可据此立即刷新观察页面
	 */
	@PostMapping("/demo-batch")
	@Operation(summary = "Create outbox demo batch", description = "Creates several demo outbox events for observation.")
	public ResponseEntity<ApiResponse<List<OutboxEventAdminVO>>> createDemoBatch() {
		return ResponseEntity.status(201).body(ApiResponse.success(outboxDebugApplicationService.createDemoBatch()));
	}
}
