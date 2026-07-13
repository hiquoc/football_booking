package com.project.common.security;

import com.project.common.constants.GlobalConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HeaderAuthenticationFilter.class);

    private final String internalGatewaySecret;
    private final boolean enforceSecret;
    private final boolean allowDocsWithoutSecret;

    public HeaderAuthenticationFilter(
            @Value("${internal.gateway.secret}") String internalGatewaySecret,
            @Value("${internal.gateway.enforce-secret:true}") boolean enforceSecret,
            @Value("${internal.gateway.allow-docs-without-secret:false}") boolean allowDocsWithoutSecret) {
        this.internalGatewaySecret = internalGatewaySecret;
        this.enforceSecret = enforceSecret;
        this.allowDocsWithoutSecret = allowDocsWithoutSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (enforceSecret && !hasValidInternalSecret(request)) {
            if (allowDocsWithoutSecret && isSwaggerRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            log.warn("request_rejected reason=invalid_internal_gateway_secret method={} path={} correlationId={}",
                    request.getMethod(), request.getRequestURI(),
                    request.getHeader(GlobalConstants.CORRELATION_HEADER_NAME));
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return;
        }

        String userIdStr = request.getHeader(GlobalConstants.HEADER_USER_ID);
        String userRole = request.getHeader(GlobalConstants.HEADER_USER_ROLE);
        String userEmail = request.getHeader(GlobalConstants.HEADER_USER_EMAIL);
        String userName = request.getHeader(GlobalConstants.HEADER_USER_NAME);

        if (StringUtils.hasText(userIdStr) && StringUtils.hasText(userRole)) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                List<SimpleGrantedAuthority> authorities = Collections
                        .singletonList(new SimpleGrantedAuthority("ROLE_" + userRole));
                UserPrincipal principal = new UserPrincipal(
                        userId,
                        StringUtils.hasText(userEmail) ? userEmail : null,
                        userRole,
                        StringUtils.hasText(userName) ? userName : null);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException e) {
                log.warn("authentication_header_rejected reason=invalid_user_id method={} path={} correlationId={}",
                        request.getMethod(), request.getRequestURI(),
                        request.getHeader(GlobalConstants.CORRELATION_HEADER_NAME));
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasValidInternalSecret(HttpServletRequest request) {
        String requestSecret = request.getHeader(GlobalConstants.HEADER_INTERNAL_SECRET);
        return StringUtils.hasText(requestSecret) && requestSecret.equals(internalGatewaySecret);
    }

    private boolean isSwaggerRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/swagger-ui.html")
                || path.equals("/v3/api-docs")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/webjars/");
    }
}
