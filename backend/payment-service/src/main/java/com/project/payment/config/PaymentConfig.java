package com.project.payment.config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Configuration @EnableConfigurationProperties(StripeProperties.class)
public class PaymentConfig {}
