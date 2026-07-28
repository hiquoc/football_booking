package com.project.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "jwt.secret=test-secret-test-secret-test-secret-test-secret"
        })
class SecurityConfigTest {

    @LocalServerPort
    private int port;

    @Test
    void publicBookingConfigDoesNotRequireAuthentication() {
        client()
                .get()
                .uri("/api/v1/bookings/config")
                .exchange()
                .expectStatus()
                .value(status -> org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }

    @Test
    void publicCommunityFeedDoesNotRequireAuthentication() {
        client()
                .get()
                .uri("/api/v1/community-posts?page=0&size=10&sortBy=newest")
                .exchange()
                .expectStatus()
                .value(status -> org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }

    @Test
    void publicCommunityDetailDoesNotRequireAuthentication() {
        client()
                .get()
                .uri("/api/v1/community-posts/11111111-1111-1111-1111-111111111111")
                .exchange()
                .expectStatus()
                .value(status -> org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }

    @Test
    void bookingCreationStillRequiresAuthentication() {
        client()
                .post()
                .uri("/api/v1/bookings")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }
}
