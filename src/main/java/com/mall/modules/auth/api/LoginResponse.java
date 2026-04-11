package com.mall.modules.auth.api;

public record LoginResponse(
	String token,
	String tokenType,
	long expiresIn,
	AuthUserResponse user
) {
}
