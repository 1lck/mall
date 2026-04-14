package com.mall.modules.user.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.user.dto.CreateUserDTO;
import com.mall.modules.user.dto.UpdateUserStatusDTO;
import com.mall.modules.user.domain.UserStatus;
import com.mall.modules.user.persistence.entity.UserEntity;
import com.mall.modules.user.persistence.mapper.UserMapper;
import com.mall.modules.user.vo.UserAdminVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * 默认后台用户管理服务，负责管理员侧的用户列表、创建和状态更新。
 */
@Service
@Transactional
public class DefaultUserManagementApplicationService implements UserManagementApplicationService {

	private final UserMapper userRepository;
	private final PasswordEncoder passwordEncoder;

	public DefaultUserManagementApplicationService(UserMapper userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 查询后台用户列表，并统一转换成后台展示对象。
	 */
	@Override
	@Transactional(readOnly = true)
	public List<UserAdminVO> listUsers() {
		return userRepository.findAllByOrderByIdDesc()
			.stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * 创建后台维护的用户账号。
	 */
	@Override
	public UserAdminVO createUser(CreateUserDTO request) {
		// 用户名在后台创建时也要先归一化，避免不同入口产生重复账号。
		String username = normalizeUsername(request.username());
		if (userRepository.existsByUsername(username)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在，请更换一个。");
		}

		// 创建账号时统一加密密码，并默认给出启用状态。
		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setNickname(request.nickname().trim());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(request.role());
		user.setStatus(UserStatus.ACTIVE);

		return toResponse(userRepository.save(user));
	}

	/**
	 * 更新指定用户的状态，例如启用或停用。
	 */
	@Override
	public UserAdminVO updateUserStatus(Long id, UpdateUserStatusDTO request) {
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User " + id + " was not found"));
		user.setStatus(request.status());
		return toResponse(userRepository.save(user));
	}

	/**
	 * 把用户实体转换成后台管理页需要的返回对象。
	 */
	private UserAdminVO toResponse(UserEntity user) {
		return new UserAdminVO(
			user.getId(),
			user.getUsername(),
			user.getNickname(),
			user.getRole(),
			user.getStatus(),
			user.getCreatedAt(),
			user.getUpdatedAt()
		);
	}

	/**
	 * 统一整理用户名格式，避免大小写和空格差异。
	 */
	private String normalizeUsername(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}
}
