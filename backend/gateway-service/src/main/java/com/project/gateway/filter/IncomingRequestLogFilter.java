package com.project.gateway.filter;

import com.project.common.constants.GlobalConstants;
import com.project.common.logging.LogContext;
import com.project.common.logging.MdcFields;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class IncomingRequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startedAt = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest();
        String requestId = LogContext.requestIdOrNew(request.getHeaders().getFirst(GlobalConstants.REQUEST_ID_HEADER_NAME));
        String userId = userId(request);
        LogContext.putRequestContext(requestId, "gateway-service");
        LogContext.putIfPresent(MdcFields.USER_ID, userId);

        log.info("request_started service=gateway-service method={} path={} remoteIp={} userId={}",
                request.getMethod(),
                request.getPath().value(),
                clientIp(request),
                userId);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    LogContext.putRequestContext(requestId, "gateway-service");
                    LogContext.putIfPresent(MdcFields.USER_ID, userId);
                    long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
                    int status = exchange.getResponse().getStatusCode() == null
                            ? 200
                            : exchange.getResponse().getStatusCode().value();
                    if (status >= 500) {
                        log.error("request_completed service=gateway-service method={} path={} status={} durationMs={} userId={}",
                                request.getMethod(), request.getPath().value(), status, durationMs, userId);
                    } else if (status >= 400) {
                        log.warn("request_completed service=gateway-service method={} path={} status={} durationMs={} userId={}",
                                request.getMethod(), request.getPath().value(), status, durationMs, userId);
                    } else {
                        log.info("request_completed service=gateway-service method={} path={} status={} durationMs={} userId={}",
                                request.getMethod(), request.getPath().value(), status, durationMs, userId);
                    }
                    MDC.clear();
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    private String clientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddress() == null
                ? "unknown"
                : request.getRemoteAddress().getAddress().getHostAddress();
    }

    private String userId(ServerHttpRequest request) {
        String headerUserId = request.getHeaders().getFirst(GlobalConstants.HEADER_USER_ID);
        return StringUtils.hasText(headerUserId) ? headerUserId : "anonymous";
    }
}
