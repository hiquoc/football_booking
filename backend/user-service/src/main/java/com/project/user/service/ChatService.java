package com.project.user.service;

import com.project.common.security.UserPrincipal;
import com.project.user.client.N8nClient;
import com.project.user.dto.ChatClientRequest;
import com.project.user.dto.ChatWebhookPayload;
import com.project.user.dto.ChatWebhookResponse;
import com.project.user.exception.ChatServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    private final N8nClient n8nClient;

    public Mono<ChatWebhookResponse> send(
            UserPrincipal user,
            String bearerToken,
            ChatClientRequest request,
            String acceptLanguage,
            String timezoneHeader) {

        if (user == null || user.id() == null || !StringUtils.hasText(user.role())) {
            throw new ChatServiceException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        if (!StringUtils.hasText(bearerToken)) {
            throw new ChatServiceException(HttpStatus.UNAUTHORIZED, "Bearer access token is required");
        }

        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        ChatWebhookPayload payload = new ChatWebhookPayload(
                new ChatWebhookPayload.User(user.id().toString(), user.role(), displayName(user)),
                new ChatWebhookPayload.Conversation(conversationId.toString(), messageId.toString()),
                new ChatWebhookPayload.Message(request.message().trim(), language(acceptLanguage)),
                new ChatWebhookPayload.Context("web", timezone(timezoneHeader))
        );

        return n8nClient.send(payload, bearerToken);
    }

    private String language(String acceptLanguage) {
        if (!StringUtils.hasText(acceptLanguage)) return DEFAULT_LANGUAGE;
        String selected = acceptLanguage.split(",", 2)[0].split(";", 2)[0].trim();
        return StringUtils.hasText(selected) ? selected.toLowerCase(Locale.ROOT) : DEFAULT_LANGUAGE;
    }

    private String timezone(String timezoneHeader) {
        if (!StringUtils.hasText(timezoneHeader)) return DEFAULT_TIMEZONE;
        try {
            return ZoneId.of(timezoneHeader.trim()).getId();
        } catch (DateTimeException exception) {
            throw new ChatServiceException(HttpStatus.BAD_REQUEST, "Invalid timezone");
        }
    }

    private String displayName(UserPrincipal user) {
        if (StringUtils.hasText(user.name())) return user.name();
        if (StringUtils.hasText(user.email())) return user.email();
        return user.id().toString();
    }
}
