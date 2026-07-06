package com.project.gateway.filter;

import com.project.common.constants.GlobalConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class InternalGatewayHeaderFilter implements GlobalFilter, Ordered {
    private final String internalGatewaySecret;

    public InternalGatewayHeaderFilter(@Value("${internal.gateway.secret}") String internalGatewaySecret) {
        this.internalGatewaySecret = internalGatewaySecret;
    }

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        var request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GlobalConstants.HEADER_INTERNAL_SECRET);
                    headers.set(GlobalConstants.HEADER_INTERNAL_SECRET, internalGatewaySecret);
                }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
