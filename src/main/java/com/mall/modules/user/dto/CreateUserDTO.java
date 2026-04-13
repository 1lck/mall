package com.mall.modules.user.dto;

import com.mall.modules.user.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
	@NotBlank(message = "must not be blank")
	@Size(min = 3, max = 50, message = "length must be between 3 and 50")
	String username,
	@NotBlank(message = "must not be blank")
	@Size(min = 2, max = 80, message = "length must be between 2 and 80")
	String nickname,
	@NotBlank(message = "must not be blank")
	@Size(min = 8, max = 64, message = "length must be between 8 and 64")
	String password,
	@NotNull(message = "must not be null")
	UserRole role
) {
}
