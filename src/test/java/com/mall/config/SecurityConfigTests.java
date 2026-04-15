package com.mall.config;

import com.mall.modules.user.domain.UserRole;
import com.mall.modules.user.domain.UserStatus;
import com.mall.modules.user.persistence.entity.UserEntity;
import com.mall.modules.user.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 安全配置单元测试，验证 JWT 转换阶段会同步检查账号实时状态。
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigTests {

	@Mock
	private UserMapper userRepository;

	@Test
	void jwtAuthenticationConverterShouldRejectDisabledUser() {
		UserEntity user = buildUser(1L, UserRole.USER, UserStatus.DISABLED);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		Converter<Jwt, AbstractAuthenticationToken> converter = new SecurityConfig()
			.jwtAuthenticationConverter(userRepository);

		assertThatThrownBy(() -> converter.convert(buildJwt(1L, "ADMIN")))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("当前账号已被停用，请联系管理员。");
	}

	@Test
	void jwtAuthenticationConverterShouldUseCurrentRoleFromDatabase() {
		UserEntity user = buildUser(1L, UserRole.USER, UserStatus.ACTIVE);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		Converter<Jwt, AbstractAuthenticationToken> converter = new SecurityConfig()
			.jwtAuthenticationConverter(userRepository);
		AbstractAuthenticationToken authenticationToken = converter.convert(buildJwt(1L, "ADMIN"));

		assertThat(authenticationToken.getAuthorities())
			.extracting("authority")
			.containsExactly("ROLE_USER");
	}

	/**
	 * 组装测试用 JWT，只保留当前鉴权逻辑依赖的最小字段。
	 */
	private Jwt buildJwt(Long userId, String role) {
		return Jwt.withTokenValue("test-token")
			.header("alg", "HS256")
			.subject("alice")
			.claim("uid", userId)
			.claim("role", role)
			.build();
	}

	/**
	 * 构造一个带最小必要信息的用户实体，方便测试鉴权转换逻辑。
	 */
	private UserEntity buildUser(Long id, UserRole role, UserStatus status) {
		UserEntity user = new UserEntity();
		setUserId(user, id);
		user.setUsername("alice");
		user.setNickname("Alice");
		user.setRole(role);
		user.setStatus(status);
		return user;
	}

	private void setUserId(UserEntity user, Long id) {
		try {
			var idField = UserEntity.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(user, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Failed to set user id for test", exception);
		}
	}
}
