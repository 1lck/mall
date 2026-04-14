package com.mall.modules.user.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.user.dto.CreateUserDTO;
import com.mall.modules.user.dto.UpdateUserStatusDTO;
import com.mall.modules.user.vo.UserAdminVO;
import com.mall.modules.user.application.UserManagementApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台用户管理控制器，提供管理员查看和维护用户账号的接口。
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin User", description = "Admin user management endpoints")
public class UserManagementController {

	private final UserManagementApplicationService userManagementApplicationService;

	public UserManagementController(UserManagementApplicationService userManagementApplicationService) {
		this.userManagementApplicationService = userManagementApplicationService;
	}

	/**
	 * 返回后台用户列表。
	 */
	@GetMapping
	@Operation(summary = "List users", description = "Returns all users for the admin console.")
	public ApiResponse<List<UserAdminVO>> listUsers() {
		return ApiResponse.success(userManagementApplicationService.listUsers());
	}

	/**
	 * 创建一个可由后台维护的用户账号。
	 */
	@PostMapping
	@Operation(summary = "Create user", description = "Creates a backend-manageable user account.")
	public ResponseEntity<ApiResponse<UserAdminVO>> createUser(@Valid @RequestBody CreateUserDTO request) {
		// 管理员新建账号同样返回 201，方便前端区分创建行为。
		return ResponseEntity.status(201).body(ApiResponse.success(userManagementApplicationService.createUser(request)));
	}

	/**
	 * 更新指定用户的启用状态。
	 */
	@PatchMapping("/{id}/status")
	@Operation(summary = "Update user status", description = "Toggles a user account between active and disabled.")
	public ApiResponse<UserAdminVO> updateUserStatus(
		@PathVariable Long id,
		@Valid @RequestBody UpdateUserStatusDTO request
	) {
		return ApiResponse.success(userManagementApplicationService.updateUserStatus(id, request));
	}
}
