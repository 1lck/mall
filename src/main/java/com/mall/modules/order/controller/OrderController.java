package com.mall.modules.order.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.order.dto.CreateOrderDTO;
import com.mall.modules.order.vo.OrderVO;
import com.mall.modules.order.dto.UpdateOrderDTO;
import com.mall.modules.order.application.OrderApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单 CRUD 接口，先提供最小可用版本，后续再扩展订单项和事件流。
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order", description = "Order CRUD endpoints")
public class OrderController {

	private final OrderApplicationService orderApplicationService;

	public OrderController(OrderApplicationService orderApplicationService) {
		this.orderApplicationService = orderApplicationService;
	}

	@PostMapping
	@Operation(summary = "Create order", description = "Creates a new order and returns the persisted result.")
	public ResponseEntity<ApiResponse<OrderVO>> createOrder(
		@AuthenticationPrincipal Jwt jwt,
		@Valid @RequestBody CreateOrderDTO request
	) {
		// 创建接口单独返回 201，更符合 REST 语义。
		return ResponseEntity.status(201)
			.body(ApiResponse.success(orderApplicationService.createOrder(readCurrentUserId(jwt), request)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get order by id", description = "Reads a single order by its primary key.")
	public ApiResponse<OrderVO> getOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return ApiResponse.success(orderApplicationService.getOrder(readCurrentUserId(jwt), isAdmin(jwt), id));
	}

	@GetMapping
	@Operation(summary = "List orders", description = "Returns all orders ordered by id descending.")
	public ApiResponse<List<OrderVO>> listOrders(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.success(orderApplicationService.listOrders(readCurrentUserId(jwt), isAdmin(jwt)));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update order", description = "Updates editable fields of an existing order.")
	public ApiResponse<OrderVO> updateOrder(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable Long id,
		@Valid @RequestBody UpdateOrderDTO request
	) {
		return ApiResponse.success(orderApplicationService.updateOrder(readCurrentUserId(jwt), isAdmin(jwt), id, request));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete order", description = "Deletes an order by id.")
	public ApiResponse<String> deleteOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		orderApplicationService.deleteOrder(readCurrentUserId(jwt), isAdmin(jwt), id);
		// 当前删除接口只返回一个简单确认结果。
		return ApiResponse.success("deleted");
	}

	private Long readCurrentUserId(Jwt jwt) {
		Object claim = jwt.getClaims().get("uid");
		if (claim instanceof Number number) {
			return number.longValue();
		}

		throw new IllegalStateException("JWT does not contain a valid uid claim");
	}

	private boolean isAdmin(Jwt jwt) {
		return "ADMIN".equals(jwt.getClaimAsString("role"));
	}
}
