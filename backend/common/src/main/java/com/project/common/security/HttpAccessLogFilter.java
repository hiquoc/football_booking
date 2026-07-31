package com.project.common.security;

import com.project.common.constants.GlobalConstants;
import com.project.common.logging.LogContext;
import com.project.common.logging.MdcFields;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

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
        Map<String, String> previousContext = org.slf4j.MDC.getCopyOfContextMap();
        String requestId = LogContext.requestIdOrNew(request.getHeader(GlobalConstants.REQUEST_ID_HEADER_NAME));

        LogContext.putRequestContext(requestId, serviceName);
        response.setHeader(GlobalConstants.REQUEST_ID_HEADER_NAME, requestId);

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
        LogContext.putIfPresent(MdcFields.USER_ID, userId);

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

            LogContext.restore(previousContext);
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
