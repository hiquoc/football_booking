package com.project.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
		"eureka.client.enabled=false",
		"jwt.secret=test-secret-test-secret-test-secret-test-secret"
})
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
