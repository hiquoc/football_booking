package com.project.user.controller;

import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import com.project.user.dto.ChatClientRequest;
import com.project.user.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public Mono<ResponseEntity<String>> chat(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody ChatClientRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            @RequestHeader(value = "X-Timezone", required = false) String timezone) {

        return chatService.send(user, bearerToken(authorization), request, acceptLanguage, timezone)
                .map(response -> ResponseEntity.status(response.status())
                        .contentType(response.contentType())
                        .body(response.body()));
    }

    private String bearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}
