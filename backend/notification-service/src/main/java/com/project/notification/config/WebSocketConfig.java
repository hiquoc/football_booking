package com.project.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import com.project.common.constants.GlobalConstants;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String internalGatewaySecret;

    public WebSocketConfig(@Value("${internal.gateway.secret}") String internalGatewaySecret) {
        this.internalGatewaySecret = internalGatewaySecret;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(gatewayHandshakeInterceptor())
                .setHandshakeHandler(new GatewayPrincipalHandshakeHandler())
                .setAllowedOriginPatterns("*");
    }

    private HandshakeInterceptor gatewayHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler handler, java.util.Map<String, Object> attributes) {
                String secret = request.getHeaders().getFirst(GlobalConstants.HEADER_INTERNAL_SECRET);
                String userId = request.getHeaders().getFirst(GlobalConstants.HEADER_USER_ID);
                if (!internalGatewaySecret.equals(secret) || userId == null) return false;
                attributes.put("userId", userId);
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler handler, Exception exception) {
            }
        };
    }

    private static final class GatewayPrincipalHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected java.security.Principal determineUser(ServerHttpRequest request, WebSocketHandler handler,
                                                        java.util.Map<String, Object> attributes) {
            String userId = (String) attributes.get("userId");
            return () -> userId;
        }
    }
}
