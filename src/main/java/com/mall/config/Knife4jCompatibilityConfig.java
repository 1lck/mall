package com.mall.config;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jSetting;
import com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer;
import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 兼容 Knife4j 4.5.0 和当前项目所需新版 springdoc 的配置。
 *
 * <p>Knife4j 内置的 customizer 还会调用旧版 springdoc 的 API，在 Spring Boot 3.5.x
 * 这套依赖下会报错。当前项目只需要正常打开文档页和保留增强配置，所以这里补一个兼容实现，
 * 保留 Knife4j 需要的扩展数据，同时绕开那段不兼容的旧逻辑。</p>
 */
@Configuration
public class Knife4jCompatibilityConfig {

	@Bean
	public Knife4jOpenApiCustomizer knife4jOpenApiCustomizer(
		Knife4jProperties knife4jProperties,
		SpringDocConfigProperties springDocConfigProperties
	) {
		return new Knife4jOpenApiCustomizer(knife4jProperties, springDocConfigProperties) {
			@Override
			public void customise(OpenAPI openAPI) {
				if (!knife4jProperties.isEnable()) {
					return;
				}

				Knife4jSetting setting = knife4jProperties.getSetting();
				OpenApiExtensionResolver resolver = new OpenApiExtensionResolver(
					setting,
					knife4jProperties.getDocuments()
				);
				resolver.start();

				// Knife4j 前端会读取这些扩展字段来渲染增强文档页面。
				Map<String, Object> knife4jExtension = new HashMap<>();
				knife4jExtension.put("x-setting", setting);
				knife4jExtension.put("x-markdownFiles", resolver.getMarkdownFiles());
				openAPI.addExtension("x-openapi", knife4jExtension);
			}
		};
	}
}
