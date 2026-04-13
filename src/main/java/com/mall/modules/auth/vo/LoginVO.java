package com.mall.modules.auth.vo;

public record LoginVO(
	String token,
	String tokenType,
	long expiresIn,
	AuthUserVO user
) {
}
