package com.mall.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

/**
 * 请求日志过滤器，为每次请求生成追踪 id 并输出统一访问日志。
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

	/** 当前类使用的日志器。 */
	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
	/** MDC 中保存追踪 id 的键名。 */
	private static final String TRACE_ID_KEY = "traceId";
	/** 返回给客户端的追踪 id 响应头名称。 */
	private static final String TRACE_ID_HEADER = "X-Trace-Id";

	/**
	 * 在请求进入和离开时记录统一日志，并把 traceId 回写到响应头。
	 */
	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		// traceId 会同时进入 MDC 和响应头，便于前后端一起定位问题。
		long startNanos = System.nanoTime();
		String traceId = UUID.randomUUID().toString();

		MDC.put(TRACE_ID_KEY, traceId);
		response.setHeader(TRACE_ID_HEADER, traceId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
			log.info(
				"请求处理完成: 请求方法={}, 请求路径={}, 状态码={}, 耗时毫秒={}, 用户={}, 追踪编号={}",
				request.getMethod(),
				request.getRequestURI(),
				response.getStatus(),
				durationMs,
				resolvePrincipalName(request.getUserPrincipal()),
				traceId
			);
			MDC.remove(TRACE_ID_KEY);
		}
	}

	/**
	 * 提取当前请求对应的登录用户名，拿不到时返回匿名标识。
	 */
	private String resolvePrincipalName(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "匿名用户";
		}

		return principal.getName();
	}
}
