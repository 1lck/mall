package com.mall.modules.user.api;

import com.mall.modules.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull(message = "must not be null") UserStatus status) {
}
