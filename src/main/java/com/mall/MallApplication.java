package com.mall;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用启动入口。
 *
 * <p>启动这个类会拉起整个后端服务，同时开启配置属性扫描，
 * 让 {@code KafkaTopicsProperties} 这类配置类自动生效。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MallApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallApplication.class, args);
	}

}
