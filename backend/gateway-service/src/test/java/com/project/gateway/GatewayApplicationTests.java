package com.project.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"jwt.secret=test-secret-test-secret-test-secret-test-secret"
})
class GatewayApplicationTests {

	@Autowired
	private RouteLocator routeLocator;

	@Test
	void contextLoads() {
		var routeIds = routeLocator.getRoutes().map(route -> route.getId()).collectList().block();
		assertThat(routeIds).contains(
				"user-service-auth", "user-service-api", "field-service-api",
				"booking-service-api", "notification-service-api",
				"notification-service-websocket");
	}

}
