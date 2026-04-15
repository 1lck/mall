package com.mall.modules.outbox.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.outbox.application.OutboxAdminApplicationService;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Outbox 后台观察控制器。
 *
 * <p>当前这一步先提供只读查询接口，
 * 让管理端能够直接观察消息投递状态、失败原因和重试时间。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/outbox-events")
@Tag(name = "Admin Outbox", description = "Admin outbox observation endpoints")
public class OutboxAdminController {

	private final OutboxAdminApplicationService outboxAdminApplicationService;

	public OutboxAdminController(OutboxAdminApplicationService outboxAdminApplicationService) {
		this.outboxAdminApplicationService = outboxAdminApplicationService;
	}

	/**
	 * 返回后台 outbox 观察列表。
	 *
	 * @param status 可选状态筛选
	 * @param keyword 可选关键字筛选
	 * @param limit 可选返回条数上限
	 * @return 适合后台直接展示的 outbox 列表
	 */
	@GetMapping
	@Operation(summary = "List outbox events", description = "Returns recent outbox events for the admin console.")
	public ApiResponse<List<OutboxEventAdminVO>> listOutboxEvents(
		@RequestParam(required = false) OutboxEventStatus status,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) Integer limit
	) {
		return ApiResponse.success(outboxAdminApplicationService.listEvents(status, keyword, limit));
	}

	/**
	 * 对指定 outbox 记录发起一次人工重发。
	 *
	 * @param id outbox 主键
	 * @return 重发申请后的最新 outbox 状态
	 */
	@PostMapping("/{id}/retry")
	@Operation(summary = "Retry outbox event", description = "Retries a FAILED or DEAD outbox event immediately.")
	public ApiResponse<OutboxEventAdminVO> retryOutboxEvent(@PathVariable Long id) {
		return ApiResponse.success(outboxAdminApplicationService.retryEvent(id));
	}
}
