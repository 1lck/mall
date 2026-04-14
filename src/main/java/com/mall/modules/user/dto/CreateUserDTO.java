package com.mall.modules.user.dto;

import com.mall.modules.user.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 后台创建用户请求体。
 */
public record CreateUserDTO(
	/** 用户登录名。 */
	@NotBlank(message = "must not be blank")
	@Size(min = 3, max = 50, message = "length must be between 3 and 50")
	String username,
	/** 用户昵称。 */
	@NotBlank(message = "must not be blank")
	@Size(min = 2, max = 80, message = "length must be between 2 and 80")
	String nickname,
	/** 登录密码原文。 */
	@NotBlank(message = "must not be blank")
	@Size(min = 8, max = 64, message = "length must be between 8 and 64")
	String password,
	/** 账号角色。 */
	@NotNull(message = "must not be null")
	UserRole role
) {
}
