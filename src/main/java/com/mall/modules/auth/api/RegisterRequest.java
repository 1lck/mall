package com.mall.modules.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	@NotBlank(message = "must not be blank")
	@Size(min = 3, max = 50, message = "length must be between 3 and 50")
	String username,
	@NotBlank(message = "must not be blank")
	@Size(min = 2, max = 80, message = "length must be between 2 and 80")
	String nickname,
	@NotBlank(message = "must not be blank")
	@Size(min = 8, max = 64, message = "length must be between 8 and 64")
	String password
) {
}
