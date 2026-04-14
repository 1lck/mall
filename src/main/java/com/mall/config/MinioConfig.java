package com.mall.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置。
 */
@Configuration
public class MinioConfig {

	/**
	 * 创建全局复用的 MinIO 客户端。
	 */
	@Bean
	public MinioClient minioClient(MinioProperties minioProperties) {
		// 上传服务统一复用一个 MinIO 客户端，避免每次调用都重新创建连接配置。
		return MinioClient.builder()
			.endpoint(minioProperties.getEndpoint())
			.credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
			.build();
	}
}
