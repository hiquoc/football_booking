package com.project.common.security;

import com.project.common.constants.GlobalConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class IncomingRequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IncomingRequestLogFilter.class);

    private final String serviceName;

    public IncomingRequestLogFilter(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String previousCorrelationId = MDC.get("correlationId");
        String correlationId = request.getHeader(GlobalConstants.CORRELATION_HEADER_NAME);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        response.setHeader(GlobalConstants.CORRELATION_HEADER_NAME, correlationId);

        log.info("incoming_request service={} method={} path={} remoteIp={}",
                serviceName, request.getMethod(), request.getRequestURI(), clientIp(request));

        try {
            filterChain.doFilter(request, response);
        } finally {
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
