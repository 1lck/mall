package com.mall.modules.auth.vo;

/**
 * 登录成功后的返回体，包含令牌信息和当前用户快照。
 */
public record LoginVO(
	/** JWT 访问令牌。 */
	String token,
	/** 令牌类型，当前固定为 Bearer。 */
	String tokenType,
	/** 令牌有效期，单位为秒。 */
	long expiresIn,
	/** 登录成功后回传的用户信息。 */
	AuthUserVO user
) {
}
