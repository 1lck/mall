package com.mall.common.exception;

import com.mall.common.api.ErrorCode;

/**
 * 业务异常。
 *
 * <p>当业务流程出现可预期错误时，抛出这个异常并交给全局异常处理器统一转换成接口响应。</p>
 */
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
