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

@Configuration
public class SecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		ObjectMapper objectMapper,
		Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
	) throws Exception {
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

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtDecoder jwtDecoder(AuthJwtProperties properties) {
		return NimbusJwtDecoder.withSecretKey(buildSecretKey(properties)).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	public JwtEncoder jwtEncoder(AuthJwtProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(buildSecretKey(properties)));
	}

	@Bean
	public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
		return new Converter<>() {
			@Override
			public AbstractAuthenticationToken convert(Jwt jwt) {
				List<GrantedAuthority> authorities = new ArrayList<>();
				String role = jwt.getClaimAsString("role");

				if (role != null && !role.isBlank()) {
					authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
				}

				return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
			}
		};
	}

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

	private SecretKey buildSecretKey(AuthJwtProperties properties) {
		return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	private String resolvePrincipalName(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "anonymous";
		}

		return principal.getName();
	}
}
