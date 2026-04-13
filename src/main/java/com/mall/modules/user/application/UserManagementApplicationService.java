package com.mall.modules.user.application;

import com.mall.modules.user.dto.CreateUserDTO;
import com.mall.modules.user.dto.UpdateUserStatusDTO;
import com.mall.modules.user.vo.UserAdminVO;

import java.util.List;

public interface UserManagementApplicationService {

	List<UserAdminVO> listUsers();

	UserAdminVO createUser(CreateUserDTO request);

	UserAdminVO updateUserStatus(Long id, UpdateUserStatusDTO request);
}
