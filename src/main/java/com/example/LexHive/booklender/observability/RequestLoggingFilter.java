package com.example.LexHive.booklender.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs one line per request (method, path, status, time taken) and tags every log line
 * written while handling that request with a request id, so all lines of one request can be found together.
 * Runs before Spring Security, so rejected requests (401/403) are logged too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
	private static final String REQUEST_ID_HEADER = "request_id";

	/**
	 * boiler plate code
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = newRequestId();
		MDC.put("requestId", requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);

		long start = System.currentTimeMillis();
		try {
			chain.doFilter(request, response);
		} finally {
			long timeTaken = System.currentTimeMillis() - start;
			log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), timeTaken);
			MDC.remove("requestId");
		}
	}

	// Health checks are called every few seconds by monitoring tools and would flood the logs.
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/actuator/health");
	}

	private static String newRequestId() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

}
