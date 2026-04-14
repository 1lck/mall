package com.mall.modules.auth.vo;

import com.mall.modules.user.domain.UserRole;
import com.mall.modules.user.domain.UserStatus;

import java.time.Instant;

/**
 * 认证模块返回的用户信息视图。
 */
public record AuthUserVO(
	/** 用户主键。 */
	Long id,
	/** 登录用户名。 */
	String username,
	/** 用户昵称。 */
	String nickname,
	/** 当前角色。 */
	UserRole role,
	/** 当前状态。 */
	UserStatus status,
	/** 账号创建时间。 */
	Instant createdAt
) {
}
