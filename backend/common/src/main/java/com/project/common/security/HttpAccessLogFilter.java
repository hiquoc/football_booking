package com.project.common.security;

import com.project.common.constants.GlobalConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** Logs request metadata and outcomes without logging credentials, headers, query strings, or bodies. */
public class HttpAccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpAccessLogFilter.class);

    private final String serviceName;

    public HttpAccessLogFilter(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String previousCorrelationId = MDC.get("correlationId");
        String correlationId = request.getHeader(GlobalConstants.CORRELATION_HEADER_NAME);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        response.setHeader(GlobalConstants.CORRELATION_HEADER_NAME, correlationId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = "anonymous";
        String role = "none";
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof UserPrincipal principal) {
                userId = principal.id().toString();
                role = principal.role();
            } else {
                userId = authentication.getName();
                role = authentication.getAuthorities().toString();
            }
        }

        log.info("request_started service={} method={} path={} remoteIp={} userId={} role={}",
                serviceName, request.getMethod(), request.getRequestURI(), clientIp(request), userId, role);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            int status = response.getStatus();
            if (status >= 500) {
                log.error("request_completed service={} method={} path={} status={} durationMs={} userId={} role={}",
                        serviceName, request.getMethod(), request.getRequestURI(), status, durationMs, userId, role);
            } else if (status >= 400) {
                log.warn("request_completed service={} method={} path={} status={} durationMs={} userId={} role={}",
                        serviceName, request.getMethod(), request.getRequestURI(), status, durationMs, userId, role);
            } else {
                log.info("request_completed service={} method={} path={} status={} durationMs={} userId={} role={}",
                        serviceName, request.getMethod(), request.getRequestURI(), status, durationMs, userId, role);
            }

            if (previousCorrelationId == null) {
                MDC.remove("correlationId");
            } else {
                MDC.put("correlationId", previousCorrelationId);
            }
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
