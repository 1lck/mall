package com.mall.modules.user.application;

import com.mall.modules.user.dto.CreateUserDTO;
import com.mall.modules.user.dto.UpdateUserStatusDTO;
import com.mall.modules.user.vo.UserAdminVO;

import java.util.List;

/**
 * 后台用户管理应用服务接口，定义用户列表、创建和状态维护能力。
 */
public interface UserManagementApplicationService {

	/**
	 * 返回后台用户列表，通常用于管理员页面展示。
	 */
	List<UserAdminVO> listUsers();

	/**
	 * 创建一个可供后台维护的用户账号。
	 */
	UserAdminVO createUser(CreateUserDTO request);

	/**
	 * 更新指定用户的启用状态。
	 */
	UserAdminVO updateUserStatus(Long id, UpdateUserStatusDTO request);
}
