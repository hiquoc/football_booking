package com.project.gateway.filter;

import com.project.common.constants.GlobalConstants;
import com.project.common.logging.LogContext;
import com.project.common.logging.MdcFields;
import org.slf4j.MDC;
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
        String requestId = StringUtils.hasText(current) ? current : UUID.randomUUID().toString();
        var request = exchange.getRequest().mutate()
                .header(GlobalConstants.REQUEST_ID_HEADER_NAME, requestId).build();
        exchange.getResponse().getHeaders().set(GlobalConstants.REQUEST_ID_HEADER_NAME, requestId);
        return chain.filter(exchange.mutate().request(request).build())
                .doOnEach(signal -> {
                    if (signal.isOnNext() || signal.isOnError() || signal.isOnComplete()) {
                        LogContext.putRequestContext(requestId, "gateway-service");
                        MDC.put(MdcFields.REQUEST_ID, requestId);
                    }
                })
                .doFinally(signalType -> MDC.clear());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
