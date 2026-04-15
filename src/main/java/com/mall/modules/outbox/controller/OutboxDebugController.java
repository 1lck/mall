package com.mall.modules.outbox.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.outbox.application.OutboxDebugApplicationService;
import com.mall.modules.outbox.dto.CreateOutboxDebugEventDTO;
import com.mall.modules.outbox.dto.DirectSendPaymentSucceededDebugDTO;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
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

	/**
	 * 清理调试功能生成的旧 outbox 数据。
	 *
	 * @return 实际删除的调试数据条数
	 */
	@PostMapping("/cleanup")
	@Operation(summary = "Cleanup outbox debug events", description = "Deletes old outbox debug data created for practice.")
	public ApiResponse<Integer> cleanupDebugEvents() {
		return ApiResponse.success(outboxDebugApplicationService.cleanupDebugEvents());
	}

	/**
	 * 按指定调试类型生成单条 outbox 消息。
	 *
	 * @param request 调试消息类型与可选聚合标识
	 * @return 刚创建的单条调试消息
	 */
	@PostMapping("/single")
	@Operation(summary = "Create single outbox debug event", description = "Creates one outbox debug event by type.")
	public ResponseEntity<ApiResponse<OutboxEventAdminVO>> createSingleEvent(
		@Valid @RequestBody CreateOutboxDebugEventDTO request
	) {
		return ResponseEntity.status(201).body(ApiResponse.success(
			outboxDebugApplicationService.createSingleEvent(request.type(), request.aggregateId())
		));
	}

	/**
	 * 直接发送支付成功 Kafka 调试消息。
	 *
	 * @param request 订单号与可选支付金额
	 * @return 已成功发出的支付成功事件
	 */
	@PostMapping("/direct-payment-succeeded")
	@Operation(summary = "Send payment succeeded message directly", description = "Sends a payment succeeded message straight to Kafka for consumer practice.")
	public ResponseEntity<ApiResponse<PaymentSucceededEvent>> sendPaymentSucceededMessage(
		@Valid @RequestBody DirectSendPaymentSucceededDebugDTO request
	) {
		return ResponseEntity.status(201).body(ApiResponse.success(
			outboxDebugApplicationService.sendPaymentSucceededMessage(request.orderNo(), request.amount())
		));
	}
}
