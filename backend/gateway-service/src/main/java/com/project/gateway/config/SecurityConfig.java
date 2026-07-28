package com.project.gateway.config;

import com.project.gateway.filter.JwtHeaderRelayFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtHeaderRelayFilter jwtHeaderRelayFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {})
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/api/v1/fields/**", "/api/v1/sub-fields/**",
                                "/api/v1/field-types/**", "/api/v1/reviews/**",
                                "/api/v1/community-posts", "/api/v1/community-posts/*",
                                "/api/v1/bookings/availability",
                                "/api/v1/bookings/config").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/api/v1/users/*/role").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, exception) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, exception) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }))
                .addFilterAt(jwtHeaderRelayFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
