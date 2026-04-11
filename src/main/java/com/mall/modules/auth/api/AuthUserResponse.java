package com.mall.modules.auth.api;

import com.mall.modules.user.domain.UserRole;
import com.mall.modules.user.domain.UserStatus;

import java.time.Instant;

public record AuthUserResponse(
	Long id,
	String username,
	String nickname,
	UserRole role,
	UserStatus status,
	Instant createdAt
) {
}
