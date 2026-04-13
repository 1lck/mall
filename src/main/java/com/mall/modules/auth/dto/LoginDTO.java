package com.mall.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
	@NotBlank(message = "must not be blank")
	String username,
	@NotBlank(message = "must not be blank")
	String password
) {
}
