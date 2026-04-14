package com.mall.modules.auth.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.auth.vo.AuthUserVO;
import com.mall.modules.auth.dto.LoginDTO;
import com.mall.modules.auth.vo.LoginVO;
import com.mall.modules.auth.dto.RegisterDTO;
import com.mall.modules.auth.application.AuthApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口控制器，对外暴露注册、登录和当前用户查询接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

	private final AuthApplicationService authApplicationService;

	public AuthController(AuthApplicationService authApplicationService) {
		this.authApplicationService = authApplicationService;
	}

	/**
	 * 注册一个新的普通用户账号。
	 */
	@PostMapping("/register")
	@Operation(summary = "Register user", description = "Creates a new basic user account for the mall.")
	public ResponseEntity<ApiResponse<AuthUserVO>> register(@Valid @RequestBody RegisterDTO request) {
		// 注册成功返回 201，明确告诉前端这是一次资源创建操作。
		return ResponseEntity.status(201).body(ApiResponse.success(authApplicationService.register(request)));
	}

	/**
	 * 校验账号密码并返回登录令牌。
	 */
	@PostMapping("/login")
	@Operation(summary = "Login", description = "Verifies the account credentials and returns a JWT token.")
	public ApiResponse<LoginVO> login(@Valid @RequestBody LoginDTO request) {
		return ApiResponse.success(authApplicationService.login(request));
	}

	/**
	 * 返回当前登录用户的资料快照。
	 */
	@GetMapping("/me")
	@Operation(summary = "Current user", description = "Reads the profile of the currently authenticated user.")
	public ApiResponse<AuthUserVO> me(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.success(authApplicationService.getCurrentUser(jwt));
	}
}
