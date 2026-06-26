package com.project.common.outbox.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/outbox")
@RequiredArgsConstructor
public class OutboxReplayController {

    private final OutboxProcessingService outboxProcessingService;

    @PostMapping("/{eventId}/replay")
    public ResponseEntity<Void> replay(@PathVariable UUID eventId) {
        outboxProcessingService.replayDeadLetter(eventId);
        return ResponseEntity.accepted().build();
    }
}
