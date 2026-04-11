package com.mall.modules.user.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.user.api.CreateUserRequest;
import com.mall.modules.user.api.UpdateUserStatusRequest;
import com.mall.modules.user.api.UserAdminResponse;
import com.mall.modules.user.domain.UserStatus;
import com.mall.modules.user.persistence.UserEntity;
import com.mall.modules.user.persistence.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class DefaultUserManagementApplicationService implements UserManagementApplicationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public DefaultUserManagementApplicationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserAdminResponse> listUsers() {
		return userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
			.stream()
			.map(this::toResponse)
			.toList();
	}

	@Override
	public UserAdminResponse createUser(CreateUserRequest request) {
		String username = normalizeUsername(request.username());
		if (userRepository.existsByUsername(username)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在，请更换一个。");
		}

		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setNickname(request.nickname().trim());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(request.role());
		user.setStatus(UserStatus.ACTIVE);

		return toResponse(userRepository.save(user));
	}

	@Override
	public UserAdminResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
		UserEntity user = userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User " + id + " was not found"));
		user.setStatus(request.status());
		return toResponse(userRepository.save(user));
	}

	private UserAdminResponse toResponse(UserEntity user) {
		return new UserAdminResponse(
			user.getId(),
			user.getUsername(),
			user.getNickname(),
			user.getRole(),
			user.getStatus(),
			user.getCreatedAt(),
			user.getUpdatedAt()
		);
	}

	private String normalizeUsername(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}
}
