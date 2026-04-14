package com.mall.common.exception;

import com.mall.common.api.ApiResponse;
import com.mall.common.api.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>控制器里抛出的异常会在这里统一转换成固定结构的 JSON 响应，
 * 避免每个接口重复写错误处理逻辑。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 处理业务异常并转换成统一响应结构。
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
		return ResponseEntity.status(resolveHttpStatus(exception.getErrorCode()))
			.body(ApiResponse.failure(exception.getErrorCode(), exception.getMessage()));
	}

	/**
	 * 处理静态资源或接口路径不存在的情况。
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiResponse.failure(ErrorCode.NOT_FOUND, exception.getMessage()));
	}

	/**
	 * 处理请求体参数校验失败的情况。
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException exception
	) {
		// 把所有字段校验错误拼成一段更适合直接返回前端的提示。
		String message = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::formatFieldError)
			.collect(Collectors.joining("; "));

		return ResponseEntity.badRequest()
			.body(ApiResponse.failure(ErrorCode.BAD_REQUEST, message));
	}

	/**
	 * 处理路径参数、查询参数等约束校验失败的情况。
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
		ConstraintViolationException exception
	) {
		return ResponseEntity.badRequest()
			.body(ApiResponse.failure(ErrorCode.BAD_REQUEST, exception.getMessage()));
	}

	/**
	 * 处理未被显式捕获的兜底异常。
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
		// 兜底异常处理，避免出现原始报错页面或未包装的栈信息。
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, exception.getMessage()));
	}

	/**
	 * 把字段校验错误格式化成更适合前端直接展示的文本。
	 */
	private String formatFieldError(FieldError fieldError) {
		return fieldError.getField() + " " + fieldError.getDefaultMessage();
	}

	/**
	 * 根据统一错误码推导要返回的 HTTP 状态码。
	 */
	private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
		return switch (errorCode) {
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
			case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
			case SUCCESS -> HttpStatus.OK;
		};
	}
}
