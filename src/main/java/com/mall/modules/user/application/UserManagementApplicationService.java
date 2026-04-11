package com.mall.modules.user.application;

import com.mall.modules.user.api.CreateUserRequest;
import com.mall.modules.user.api.UpdateUserStatusRequest;
import com.mall.modules.user.api.UserAdminResponse;

import java.util.List;

public interface UserManagementApplicationService {

	List<UserAdminResponse> listUsers();

	UserAdminResponse createUser(CreateUserRequest request);

	UserAdminResponse updateUserStatus(Long id, UpdateUserStatusRequest request);
}
