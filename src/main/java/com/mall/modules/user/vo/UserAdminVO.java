package com.mall.modules.user.vo;

import com.mall.modules.user.domain.UserRole;
import com.mall.modules.user.domain.UserStatus;

import java.time.Instant;

/**
 * 后台用户管理页使用的用户视图对象。
 */
public record UserAdminVO(
	/** 用户主键。 */
	Long id,
	/** 登录用户名。 */
	String username,
	/** 展示昵称。 */
	String nickname,
	/** 账号角色。 */
	UserRole role,
	/** 账号状态。 */
	UserStatus status,
	/** 创建时间。 */
	Instant createdAt,
	/** 最近更新时间。 */
	Instant updatedAt
) {
}
