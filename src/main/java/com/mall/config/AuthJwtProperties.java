package com.mall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.auth.jwt")
public record AuthJwtProperties(String secret, long expiresInSeconds) {
}
