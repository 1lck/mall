package com.mall.modules.auth.application;

import com.mall.modules.auth.vo.AuthUserVO;
import com.mall.modules.auth.dto.LoginDTO;
import com.mall.modules.auth.vo.LoginVO;
import com.mall.modules.auth.dto.RegisterDTO;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthApplicationService {

	AuthUserVO register(RegisterDTO request);

	LoginVO login(LoginDTO request);

	AuthUserVO getCurrentUser(Jwt jwt);
}
