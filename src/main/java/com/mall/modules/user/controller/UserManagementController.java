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

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin User", description = "Admin user management endpoints")
public class UserManagementController {

	private final UserManagementApplicationService userManagementApplicationService;

	public UserManagementController(UserManagementApplicationService userManagementApplicationService) {
		this.userManagementApplicationService = userManagementApplicationService;
	}

	@GetMapping
	@Operation(summary = "List users", description = "Returns all users for the admin console.")
	public ApiResponse<List<UserAdminVO>> listUsers() {
		return ApiResponse.success(userManagementApplicationService.listUsers());
	}

	@PostMapping
	@Operation(summary = "Create user", description = "Creates a backend-manageable user account.")
	public ResponseEntity<ApiResponse<UserAdminVO>> createUser(@Valid @RequestBody CreateUserDTO request) {
		return ResponseEntity.status(201).body(ApiResponse.success(userManagementApplicationService.createUser(request)));
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Update user status", description = "Toggles a user account between active and disabled.")
	public ApiResponse<UserAdminVO> updateUserStatus(
		@PathVariable Long id,
		@Valid @RequestBody UpdateUserStatusDTO request
	) {
		return ApiResponse.success(userManagementApplicationService.updateUserStatus(id, request));
	}
}
