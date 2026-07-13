package com.project.payment.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;
@Configuration public class OpenApiConfig {
    @Bean OpenAPI paymentOpenApi() { return new OpenAPI().info(new Info().title("Payment Service API").version("v1")); }
}
