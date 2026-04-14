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

	/** MinIO 服务地址。 */
	private String endpoint;
	/** 访问账号。 */
	private String accessKey;
	/** 访问密钥。 */
	private String secretKey;
	/** 默认上传 bucket。 */
	private String bucket;
	/** 对外访问图片的基础地址。 */
	private String publicBaseUrl;

	/**
	 * 返回 MinIO 服务地址。
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * 设置 MinIO 服务地址。
	 */
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	/**
	 * 返回访问账号。
	 */
	public String getAccessKey() {
		return accessKey;
	}

	/**
	 * 设置访问账号。
	 */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	/**
	 * 返回访问密钥。
	 */
	public String getSecretKey() {
		return secretKey;
	}

	/**
	 * 设置访问密钥。
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	/**
	 * 返回默认 bucket 名称。
	 */
	public String getBucket() {
		return bucket;
	}

	/**
	 * 设置默认 bucket 名称。
	 */
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	/**
	 * 返回对外访问图片的基础地址。
	 */
	public String getPublicBaseUrl() {
		return publicBaseUrl;
	}

	/**
	 * 设置对外访问图片的基础地址。
	 */
	public void setPublicBaseUrl(String publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}
}
