package com.mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 本地前后端分离开发时的跨域配置。
 *
 * <p>当前前端运行在 Vite 默认开发端口 5173，后端运行在 8080。
 * 这里显式放行本地前端访问 API，避免浏览器把真实请求拦成 CORS 错误。</p>
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

	private static final List<String> ALLOWED_ORIGIN_PATTERNS = List.of(
		"http://localhost:[*]",
		"http://127.0.0.1:[*]",
		"http://192.168.*.*:[*]",
		"http://10.*.*.*:[*]",
		"http://172.16.*.*:[*]",
		"http://172.17.*.*:[*]",
		"http://172.18.*.*:[*]",
		"http://172.19.*.*:[*]",
		"http://172.20.*.*:[*]",
		"http://172.21.*.*:[*]",
		"http://172.22.*.*:[*]",
		"http://172.23.*.*:[*]",
		"http://172.24.*.*:[*]",
		"http://172.25.*.*:[*]",
		"http://172.26.*.*:[*]",
		"http://172.27.*.*:[*]",
		"http://172.28.*.*:[*]",
		"http://172.29.*.*:[*]",
		"http://172.30.*.*:[*]",
		"http://172.31.*.*:[*]"
	);

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS.toArray(String[]::new))
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.exposedHeaders("X-Trace-Id")
			.maxAge(3600);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of("X-Trace-Id"));
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}
}
