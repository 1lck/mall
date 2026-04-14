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

/**
 * 默认认证应用服务，负责注册、登录和当前登录用户查询。
 */
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

	/**
	 * 创建一个新的普通用户账号。
	 */
	@Override
	public AuthUserVO register(RegisterDTO request) {
		// 注册入口统一做用户名归一化，避免大小写差异导致重复账号。
		String username = normalizeUsername(request.username());
		if (userRepository.existsByUsername(username)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在，请更换一个。");
		}

		// 当前注册流程只创建最基础的普通用户账号，角色和状态由后端强制给定。
		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setNickname(request.nickname().trim());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.ACTIVE);

		return toUserResponse(userRepository.save(user));
	}

	/**
	 * 校验用户名和密码，并在通过校验后生成 JWT 令牌。
	 */
	@Override
	@Transactional(readOnly = true)
	public LoginVO login(LoginDTO request) {
		// 登录时沿用和注册一致的用户名归一化规则，确保读取口径统一。
		String username = normalizeUsername(request.username());
		UserEntity user = userRepository.findByUsername(username)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误。"));

		// 被停用账号不允许继续登录，避免后台停用后仍能拿到新令牌。
		if (user.getStatus() == UserStatus.DISABLED) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "该账号已被停用，请联系管理员。");
		}

		// 密码比对失败时继续返回统一错误，避免暴露账号存在性。
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误。");
		}

		// JWT 中保留 uid、昵称和角色，方便后续接口直接从令牌里取登录上下文。
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

	/**
	 * 通过 JWT 中的 uid 声明读取当前登录用户资料。
	 */
	@Override
	@Transactional(readOnly = true)
	public AuthUserVO getCurrentUser(Jwt jwt) {
		// 这里显式校验 uid 类型，避免令牌结构异常时继续往下查询数据库。
		Object uidClaim = jwt.getClaims().get("uid");
		if (!(uidClaim instanceof Number uidNumber)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再访问。");
		}

		UserEntity user = userRepository.findById(uidNumber.longValue())
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再访问。"));

		return toUserResponse(user);
	}

	/**
	 * 把用户实体转换成认证模块使用的返回对象。
	 */
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

	/**
	 * 统一整理用户名格式，避免账号大小写和首尾空格带来歧义。
	 */
	private String normalizeUsername(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}
}
