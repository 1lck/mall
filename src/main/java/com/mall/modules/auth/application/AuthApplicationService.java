package com.mall.modules.auth.application;

import com.mall.modules.auth.api.AuthUserResponse;
import com.mall.modules.auth.api.LoginRequest;
import com.mall.modules.auth.api.LoginResponse;
import com.mall.modules.auth.api.RegisterRequest;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthApplicationService {

	AuthUserResponse register(RegisterRequest request);

	LoginResponse login(LoginRequest request);

	AuthUserResponse getCurrentUser(Jwt jwt);
}
