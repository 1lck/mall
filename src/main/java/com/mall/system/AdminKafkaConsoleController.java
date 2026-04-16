package com.mall.system;

import com.mall.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 Kafka 控制台接口。
 *
 * <p>提供面向调试页的 Kafka 运行态数据，
 * 让前端可以直接展示 topic、lag 和最近消息，而不必自己拼底层概念。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/kafka-console")
@Tag(name = "Admin Kafka Console", description = "Admin Kafka observation endpoints")
public class AdminKafkaConsoleController {

	private final AdminKafkaConsoleService adminKafkaConsoleService;

	public AdminKafkaConsoleController(AdminKafkaConsoleService adminKafkaConsoleService) {
		this.adminKafkaConsoleService = adminKafkaConsoleService;
	}

	/**
	 * 返回 Kafka 控制台总览。
	 */
	@GetMapping("/overview")
	@Operation(summary = "Read Kafka console overview", description = "Returns topic status, partition lag and consumer group progress.")
	public ApiResponse<AdminKafkaConsoleOverviewResponse> getOverview() {
		return ApiResponse.success(adminKafkaConsoleService.getOverview());
	}

	/**
	 * 返回指定 topic 最近消息。
	 */
	@GetMapping("/messages")
	@Operation(summary = "Read recent Kafka messages", description = "Returns recent records from a topic, optionally filtered by partition.")
	public ApiResponse<AdminKafkaTopicMessagesResponse> getMessages(
		@RequestParam String topic,
		@RequestParam(required = false) Integer partition,
		@RequestParam(required = false) Integer limit
	) {
		return ApiResponse.success(adminKafkaConsoleService.getTopicMessages(topic, partition, limit));
	}
}
