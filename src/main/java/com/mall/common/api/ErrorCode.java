package com.mall.common.api;

/**
 * 统一的业务错误码定义。
 */
public enum ErrorCode {

	/** 请求处理成功。 */
	SUCCESS("SUCCESS", "Request succeeded"),
	/** 请求参数不合法。 */
	BAD_REQUEST("BAD_REQUEST", "Request is invalid"),
	/** 当前请求尚未完成登录认证。 */
	UNAUTHORIZED("UNAUTHORIZED", "请先登录后再访问。"),
	/** 已登录但没有权限访问目标资源。 */
	FORBIDDEN("FORBIDDEN", "当前账号没有权限执行该操作。"),
	/** 目标资源不存在。 */
	NOT_FOUND("NOT_FOUND", "Requested resource was not found"),
	/** 服务端出现未预期异常。 */
	INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected server error");

	/** 错误码字符串。 */
	private final String code;
	/** 默认错误消息。 */
	private final String message;

	/**
	 * 创建一个统一错误码枚举项。
	 */
	ErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

	/**
	 * 返回错误码字符串。
	 */
	public String getCode() {
		return code;
	}

	/**
	 * 返回默认错误消息。
	 */
	public String getMessage() {
		return message;
	}
}
