package com.mall.common.api;

/**
 * 统一的 HTTP 响应包装对象。
 *
 * <p>项目里的接口无论成功还是失败，都尽量返回同一种外层结构，
 * 这样前端处理起来会更稳定。</p>
 */
public record ApiResponse<T>(
	/** 本次调用是否成功。 */
	boolean success,
	/** 统一错误码或成功码。 */
	String code,
	/** 给前端展示或排查问题的消息。 */
	String message,
	/** 真实返回数据，失败时通常为 {@code null}。 */
	T data
) {

	/**
	 * 构造一个成功响应。
	 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
	}

	/**
	 * 构造一个失败响应。
	 */
	public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
		return new ApiResponse<>(false, errorCode.getCode(), message, null);
	}
}
