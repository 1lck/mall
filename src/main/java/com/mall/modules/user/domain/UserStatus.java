package com.mall.modules.user.domain;

/**
 * 用户账号状态枚举，描述账号是否允许继续使用。
 */
public enum UserStatus {
	/** 正常启用，可以登录和访问系统。 */
	ACTIVE,
	/** 已被停用，不允许继续登录。 */
	DISABLED
}
