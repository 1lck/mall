package com.mall.common.api;

/**
 * 统一的业务错误码定义。
 */
public enum ErrorCode {

	SUCCESS("SUCCESS", "Request succeeded"),
	BAD_REQUEST("BAD_REQUEST", "Request is invalid"),
	UNAUTHORIZED("UNAUTHORIZED", "请先登录后再访问。"),
	FORBIDDEN("FORBIDDEN", "当前账号没有权限执行该操作。"),
	NOT_FOUND("NOT_FOUND", "Requested resource was not found"),
	INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected server error");

	private final String code;
	private final String message;

	ErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
