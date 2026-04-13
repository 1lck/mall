package com.mall.modules.user.dto;

import com.mall.modules.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusDTO(@NotNull(message = "must not be null") UserStatus status) {
}
