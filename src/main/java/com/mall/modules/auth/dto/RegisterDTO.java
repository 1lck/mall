package com.mall.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体，描述创建普通用户时需要提交的信息。
 */
public record RegisterDTO(
	/** 新账号的登录用户名。 */
	@NotBlank(message = "must not be blank")
	@Size(min = 3, max = 50, message = "length must be between 3 and 50")
	String username,
	/** 对外展示的昵称。 */
	@NotBlank(message = "must not be blank")
	@Size(min = 2, max = 80, message = "length must be between 2 and 80")
	String nickname,
	/** 登录密码原文，进入服务层后会被加密存储。 */
	@NotBlank(message = "must not be blank")
	@Size(min = 8, max = 64, message = "length must be between 8 and 64")
	String password
) {
}
