package com.mall.modules.auth.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.config.AuthJwtProperties;
import com.mall.modules.auth.vo.AuthUserVO;
import com.mall.modules.auth.dto.LoginDTO;
import com.mall.modules.auth.vo.LoginVO;
import com.mall.modules.auth.dto.RegisterDTO;
import com.mall.modules.user.domain.UserRole;
import com.mall.modules.user.domain.UserStatus;
import com.mall.modules.user.persistence.entity.UserEntity;
import com.mall.modules.user.persistence.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@Transactional
public class DefaultAuthApplicationService implements AuthApplicationService {

	private final UserMapper userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final AuthJwtProperties authJwtProperties;

	public DefaultAuthApplicationService(
		UserMapper userRepository,
		PasswordEncoder passwordEncoder,
		JwtEncoder jwtEncoder,
		AuthJwtProperties authJwtProperties
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.authJwtProperties = authJwtProperties;
	}

	@Override
	public AuthUserVO register(RegisterDTO request) {
		String username = normalizeUsername(request.username());
		if (userRepository.existsByUsername(username)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在，请更换一个。");
		}

		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setNickname(request.nickname().trim());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.ACTIVE);

		return toUserResponse(userRepository.save(user));
	}

	@Override
	@Transactional(readOnly = true)
	public LoginVO login(LoginDTO request) {
		String username = normalizeUsername(request.username());
		UserEntity user = userRepository.findByUsername(username)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误。"));

		if (user.getStatus() == UserStatus.DISABLED) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "该账号已被停用，请联系管理员。");
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误。");
		}

		Instant issuedAt = Instant.now();
		long expiresIn = authJwtProperties.expiresInSeconds();
		Instant expiresAt = issuedAt.plusSeconds(expiresIn);
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(user.getUsername())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.claim("uid", user.getId())
			.claim("nickname", user.getNickname())
			.claim("role", user.getRole().name())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		return new LoginVO(token, "Bearer", expiresIn, toUserResponse(user));
	}

	@Override
	@Transactional(readOnly = true)
	public AuthUserVO getCurrentUser(Jwt jwt) {
		Object uidClaim = jwt.getClaims().get("uid");
		if (!(uidClaim instanceof Number uidNumber)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再访问。");
		}

		UserEntity user = userRepository.findById(uidNumber.longValue())
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再访问。"));

		return toUserResponse(user);
	}

	private AuthUserVO toUserResponse(UserEntity user) {
		return new AuthUserVO(
			user.getId(),
			user.getUsername(),
			user.getNickname(),
			user.getRole(),
			user.getStatus(),
			user.getCreatedAt()
		);
	}

	private String normalizeUsername(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}
}
