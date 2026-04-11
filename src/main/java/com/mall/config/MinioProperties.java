package com.mall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 相关配置。
 *
 * <p>把对象存储的连接地址、账号和 bucket 名称统一收口到这里，
 * 后面上传服务直接依赖这份配置即可。</p>
 */
@ConfigurationProperties(prefix = "mall.minio")
public class MinioProperties {

	private String endpoint;
	private String accessKey;
	private String secretKey;
	private String bucket;
	private String publicBaseUrl;

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getPublicBaseUrl() {
		return publicBaseUrl;
	}

	public void setPublicBaseUrl(String publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}
}
