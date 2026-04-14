package com.mall.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.api.ApiResponse;
import com.mall.common.api.ErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Spring Security 配置，负责鉴权规则、JWT 编解码和统一错误响应。
 */
@Configuration
public class SecurityConfig {

	/** 安全相关日志输出器。 */
	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	/**
	 * 构建系统使用的安全过滤链。
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		ObjectMapper objectMapper,
		Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
	) throws Exception {
		// 这个项目使用无状态 JWT 鉴权，所以关闭 Session 并启用资源服务器模式。
		http
			.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint((request, response, exception) -> {
					log.warn(
						"Security authentication failed: method={}, path={}, status=401, message={}",
						request.getMethod(),
						request.getRequestURI(),
						exception.getMessage()
					);
					writeErrorResponse(response, objectMapper, 401, ErrorCode.UNAUTHORIZED, "请先登录后再访问。");
				})
				.accessDeniedHandler((request, response, exception) -> {
					log.warn(
						"Security access denied: method={}, path={}, user={}, status=403, message={}",
						request.getMethod(),
						request.getRequestURI(),
						resolvePrincipalName(request.getUserPrincipal()),
						exception.getMessage()
					);
					writeErrorResponse(response, objectMapper, 403, ErrorCode.FORBIDDEN, "当前账号没有权限执行该操作。");
				})
			)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
				.requestMatchers("/api/v1/system/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/doc.html")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/*").permitAll()
				.requestMatchers("/api/v1/admin/users/**").hasRole("ADMIN")
				.requestMatchers("/api/v1/admin/dashboard/**").hasRole("ADMIN")
				.requestMatchers("/api/v1/products/images/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/v1/products").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/api/v1/products/*").hasRole("ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/api/v1/products/*").hasRole("ADMIN")
				.requestMatchers("/api/v1/orders/**").authenticated()
				.requestMatchers("/api/v1/auth/me").authenticated()
				.anyRequest().permitAll()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

		return http.build();
	}

	/**
	 * 提供统一的密码加密器。
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * 根据配置中的密钥构建 JWT 解码器。
	 */
	@Bean
	public JwtDecoder jwtDecoder(AuthJwtProperties properties) {
		return NimbusJwtDecoder.withSecretKey(buildSecretKey(properties)).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/**
	 * 根据配置中的密钥构建 JWT 编码器。
	 */
	@Bean
	public JwtEncoder jwtEncoder(AuthJwtProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(buildSecretKey(properties)));
	}

	/**
	 * 把 JWT 中的角色信息转换成 Spring Security 可识别的权限集合。
	 */
	@Bean
	public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
		return new Converter<>() {
			@Override
			public AbstractAuthenticationToken convert(Jwt jwt) {
				List<GrantedAuthority> authorities = new ArrayList<>();
				String role = jwt.getClaimAsString("role");

				// 只有令牌里明确带了角色时才注入权限，避免构造出无效角色。
				if (role != null && !role.isBlank()) {
					authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
				}

				return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
			}
		};
	}

	/**
	 * 按统一响应格式向客户端写入认证或鉴权失败信息。
	 */
	private void writeErrorResponse(
		jakarta.servlet.http.HttpServletResponse response,
		ObjectMapper objectMapper,
		int status,
		ErrorCode errorCode,
		String message
	) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(errorCode, message));
	}

	/**
	 * 把配置里的字符串密钥转换成 HMAC-SHA256 所需的 SecretKey。
	 */
	private SecretKey buildSecretKey(AuthJwtProperties properties) {
		return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	/**
	 * 提取当前请求的用户名，用于日志输出。
	 */
	private String resolvePrincipalName(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "anonymous";
		}

		return principal.getName();
	}
}
