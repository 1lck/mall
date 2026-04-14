package com.mall.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一管理 OpenAPI 文档的基础信息，springdoc 和 Knife4j 都会读取这里的内容。
 *
 * <p>这个类本身不会暴露接口文档地址，真正暴露地址的是文档相关 starter。
 * 它只负责控制页面上显示的标题、描述和版本号。</p>
 */
@Configuration
public class OpenApiConfig {

	/**
	 * 定义 OpenAPI 文档首页展示的基础信息。
	 */
	@Bean
	public OpenAPI mallOpenAPI() {
		// 这里定义的是文档首页顶部展示的标题、描述和版本号。
		return new OpenAPI().info(new Info()
			.title("Mall API")
			.description("商城后端练习项目的接口文档。")
			.version("v1"));
	}
}
