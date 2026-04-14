package com.mall.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求体，承载用户名和密码。
 */
public record LoginDTO(
	/** 登录用户名。 */
	@NotBlank(message = "must not be blank")
	String username,
	/** 登录密码。 */
	@NotBlank(message = "must not be blank")
	String password
) {
}
