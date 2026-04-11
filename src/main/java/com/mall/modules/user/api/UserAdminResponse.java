package com.mall.modules.user.api;

import com.mall.modules.user.domain.UserRole;
import com.mall.modules.user.domain.UserStatus;

import java.time.Instant;

public record UserAdminResponse(
	Long id,
	String username,
	String nickname,
	UserRole role,
	UserStatus status,
	Instant createdAt,
	Instant updatedAt
) {
}
