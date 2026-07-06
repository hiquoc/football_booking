package com.project.gateway.filter;

import com.project.common.constants.GlobalConstants;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String current = exchange.getRequest().getHeaders().getFirst(GlobalConstants.CORRELATION_HEADER_NAME);
        String correlationId = StringUtils.hasText(current) ? current : UUID.randomUUID().toString();
        var request = exchange.getRequest().mutate()
                .header(GlobalConstants.CORRELATION_HEADER_NAME, correlationId).build();
        exchange.getResponse().getHeaders().set(GlobalConstants.CORRELATION_HEADER_NAME, correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
