package com.mall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 认证配置，定义签名密钥和令牌有效期。
 */
@ConfigurationProperties(prefix = "mall.auth.jwt")
public record AuthJwtProperties(
	/** JWT 签名密钥。 */
	String secret,
	/** 令牌过期时间，单位为秒。 */
	long expiresInSeconds
) {
}
