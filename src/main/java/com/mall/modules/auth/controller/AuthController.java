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

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

	private final AuthApplicationService authApplicationService;

	public AuthController(AuthApplicationService authApplicationService) {
		this.authApplicationService = authApplicationService;
	}

	@PostMapping("/register")
	@Operation(summary = "Register user", description = "Creates a new basic user account for the mall.")
	public ResponseEntity<ApiResponse<AuthUserVO>> register(@Valid @RequestBody RegisterDTO request) {
		return ResponseEntity.status(201).body(ApiResponse.success(authApplicationService.register(request)));
	}

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Verifies the account credentials and returns a JWT token.")
	public ApiResponse<LoginVO> login(@Valid @RequestBody LoginDTO request) {
		return ApiResponse.success(authApplicationService.login(request));
	}

	@GetMapping("/me")
	@Operation(summary = "Current user", description = "Reads the profile of the currently authenticated user.")
	public ApiResponse<AuthUserVO> me(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.success(authApplicationService.getCurrentUser(jwt));
	}
}
