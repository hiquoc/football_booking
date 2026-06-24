package com.project.field.config;

import com.project.common.constants.GlobalConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            template.header(GlobalConstants.CORRELATION_HEADER_NAME, correlationId);
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userId = request.getHeader(GlobalConstants.HEADER_USER_ID);
            String role = request.getHeader(GlobalConstants.HEADER_USER_ROLE);
            String email = request.getHeader(GlobalConstants.HEADER_USER_EMAIL);

            if (userId != null) {
                template.header(GlobalConstants.HEADER_USER_ID, userId);
            }
            if (role != null) {
                template.header(GlobalConstants.HEADER_USER_ROLE, role);
            }
            if (email != null) {
                template.header(GlobalConstants.HEADER_USER_EMAIL, email);
            }
        }
    }
}
