package com.mall.modules.user.vo;

import com.mall.modules.user.domain.UserRole;
import com.mall.modules.user.domain.UserStatus;

import java.time.Instant;

public record UserAdminVO(
	Long id,
	String username,
	String nickname,
	UserRole role,
	UserStatus status,
	Instant createdAt,
	Instant updatedAt
) {
}
