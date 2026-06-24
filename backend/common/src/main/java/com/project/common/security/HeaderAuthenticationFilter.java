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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userIdStr = request.getHeader(GlobalConstants.HEADER_USER_ID);
        String userRole = request.getHeader(GlobalConstants.HEADER_USER_ROLE);
        String userEmail = request.getHeader(GlobalConstants.HEADER_USER_EMAIL);

        if (StringUtils.hasText(userIdStr) && StringUtils.hasText(userRole)) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                List<SimpleGrantedAuthority> authorities = Collections
                        .singletonList(new SimpleGrantedAuthority("ROLE_" + userRole));
                UserPrincipal principal = new UserPrincipal(
                        userId,
                        StringUtils.hasText(userEmail) ? userEmail : null,
                        userRole);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException e) {
                // Ignore format errors and proceed with an unauthenticated request.
            }
        }

        filterChain.doFilter(request, response);
    }
}
