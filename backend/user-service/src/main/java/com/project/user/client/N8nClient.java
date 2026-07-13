package com.project.user.client;

import com.project.user.config.N8nProperties;
import com.project.user.dto.ChatWebhookPayload;
import com.project.user.dto.ChatWebhookResponse;
import com.project.user.exception.ChatServiceException;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class N8nClient {

    private final WebClient n8nWebClient;
    private final N8nProperties properties;

    public Mono<ChatWebhookResponse> send(ChatWebhookPayload payload, String bearerToken) {
        return n8nWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .header("X-N8N-Key", properties.key())
                .bodyValue(payload)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new ChatWebhookResponse(
                                response.statusCode().value(),
                                body,
                                response.headers().contentType().orElse(MediaType.APPLICATION_JSON))))
                .timeout(properties.timeout())
                .retryWhen(Retry.fixedDelay(properties.maxRetries(), Duration.ofMillis(250))
                        .filter(this::isTransientNetworkFailure)
                        .onRetryExhaustedThrow((retrySpec, signal) -> signal.failure()))
                .onErrorMap(this::toChatException);
    }

    private boolean isTransientNetworkFailure(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        return cause instanceof IOException
                || cause instanceof WebClientRequestException
                || cause instanceof TimeoutException
                || cause instanceof ReadTimeoutException;
    }

    private Throwable toChatException(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof ChatServiceException) {
            return cause;
        }
        if (cause instanceof TimeoutException || cause instanceof ReadTimeoutException) {
            return new ChatServiceException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "The AI assistant took too long to respond");
        }
        if (isTransientNetworkFailure(cause)) {
            return new ChatServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI assistant is currently unavailable");
        }
        return throwable;
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && (current instanceof RuntimeException || current instanceof WebClientRequestException)) {
            current = current.getCause();
        }
        return current;
    }
}
