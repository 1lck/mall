package com.mall.modules.auth.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank(message = "must not be blank")
	String username,
	@NotBlank(message = "must not be blank")
	String password
) {
}
