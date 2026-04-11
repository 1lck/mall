package com.mall.common.api;

/**
 * 统一的 HTTP 响应包装对象。
 *
 * <p>项目里的接口无论成功还是失败，都尽量返回同一种外层结构，
 * 这样前端处理起来会更稳定。</p>
 */
public record ApiResponse<T>(boolean success, String code, String message, T data) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
	}

	public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
		return new ApiResponse<>(false, errorCode.getCode(), message, null);
	}
}
