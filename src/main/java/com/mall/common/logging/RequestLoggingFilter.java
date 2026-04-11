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

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
	private static final String TRACE_ID_KEY = "traceId";
	private static final String TRACE_ID_HEADER = "X-Trace-Id";

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		long startNanos = System.nanoTime();
		String traceId = UUID.randomUUID().toString();

		MDC.put(TRACE_ID_KEY, traceId);
		response.setHeader(TRACE_ID_HEADER, traceId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
			log.info(
				"HTTP request completed: method={}, path={}, status={}, durationMs={}, user={}, traceId={}",
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

	private String resolvePrincipalName(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "anonymous";
		}

		return principal.getName();
	}
}
