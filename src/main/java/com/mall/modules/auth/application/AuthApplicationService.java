package com.mall.modules.auth.application;

import com.mall.modules.auth.vo.AuthUserVO;
import com.mall.modules.auth.dto.LoginDTO;
import com.mall.modules.auth.vo.LoginVO;
import com.mall.modules.auth.dto.RegisterDTO;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 认证应用服务接口，定义注册、登录和当前用户查询这三类认证能力。
 */
public interface AuthApplicationService {

	/**
	 * 注册普通用户账号，并返回创建后的基础用户信息。
	 */
	AuthUserVO register(RegisterDTO request);

	/**
	 * 校验登录凭证并签发访问令牌。
	 */
	LoginVO login(LoginDTO request);

	/**
	 * 根据当前请求携带的 JWT 解析出登录用户信息。
	 */
	AuthUserVO getCurrentUser(Jwt jwt);
}
