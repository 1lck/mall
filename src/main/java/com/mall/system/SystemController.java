package com.mall.system;

import com.mall.common.api.ApiResponse;
import com.mall.config.KafkaTopicsProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统诊断接口。
 *
 * <p>主要用来确认服务是否正常启动，以及查看当前项目的模块和配置概览。</p>
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "System inspection and smoke-check endpoints")
public class SystemController {

	private final String applicationName;
	private final KafkaTopicsProperties kafkaTopicsProperties;

	public SystemController(
		@Value("${spring.application.name}") String applicationName,
		KafkaTopicsProperties kafkaTopicsProperties
	) {
		this.applicationName = applicationName;
		this.kafkaTopicsProperties = kafkaTopicsProperties;
	}

	@GetMapping("/ping")
	// 这类 Swagger 注解主要是为了让文档页面更容易读。
	@Operation(summary = "Ping the service", description = "Returns a simple pong response to confirm the web layer is alive.")
	public ApiResponse<String> ping() {
		// 最简单的探针接口，只要能返回就说明 Web 层已经工作。
		return ApiResponse.success("pong");
	}

	@GetMapping("/overview")
	@Operation(summary = "Read system overview", description = "Returns enabled modules and the current Kafka-related configuration.")
	public ApiResponse<SystemOverviewResponse> overview() {
		// 组装当前模块和 Kafka 配置的快照信息，方便快速查看项目状态。
		SystemOverviewResponse overview = new SystemOverviewResponse(
			applicationName,
			List.of("product", "cart", "order", "inventory", "payment", "search"),
			new SystemOverviewResponse.KafkaView(
				kafkaTopicsProperties.isEnabled(),
				kafkaTopicsProperties.getBootstrapServers(),
				kafkaTopicsProperties.getConsumerGroup(),
				List.of(
					kafkaTopicsProperties.getTopics().getOrderCreated(),
					kafkaTopicsProperties.getTopics().getOrderCancelled(),
					kafkaTopicsProperties.getTopics().getInventoryReserved(),
					kafkaTopicsProperties.getTopics().getInventoryReleased()
				)
			)
		);

		return ApiResponse.success(overview);
	}
}
