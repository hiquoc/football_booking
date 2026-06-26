package com.project.common.inbox.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/inbox")
@RequiredArgsConstructor
public class InboxReplayController {

    private final InboxProcessingService inboxProcessingService;

    @PostMapping("/{eventId}/replay")
    public ResponseEntity<Void> replay(@PathVariable UUID eventId) {
        inboxProcessingService.replayFailed(eventId);
        return ResponseEntity.accepted().build();
    }
}
