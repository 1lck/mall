package com.mall.modules.user.dto;

import com.mall.modules.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 用户状态更新请求体。
 */
public record UpdateUserStatusDTO(
	/** 目标状态。 */
	@NotNull(message = "must not be null") UserStatus status
) {
}
