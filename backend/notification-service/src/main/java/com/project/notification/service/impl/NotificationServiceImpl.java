package com.project.notification.service.impl;

import com.project.common.dto.PageResponse;
import com.project.common.exception.NotFoundException;
import com.project.notification.dto.NotificationRequest;
import com.project.notification.dto.NotificationResponse;
import com.project.notification.entity.Notification;
import com.project.notification.enums.NotificationChannel;
import com.project.notification.mapper.NotificationMapper;
import com.project.notification.repository.NotificationRepository;
import com.project.notification.service.NotificationSender;
import com.project.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            SimpMessagingTemplate messagingTemplate,
            List<NotificationSender> senderList) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.messagingTemplate = messagingTemplate;
        this.senders = new EnumMap<>(NotificationChannel.class);
        senderList.forEach(sender -> this.senders.put(sender.getChannel(), sender));
    }

    @Override
    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        Notification saved = notificationRepository.save(Notification.builder()
                .userId(request.getUserId())
                .code(request.getCode())
                .title(request.getTitle())
                .payload(request.getPayload())
                .isRead(false)
                .build());
        NotificationResponse response = notificationMapper.toResponse(saved);

        List<NotificationChannel> channels = request.getChannels() == null
                ? List.of(NotificationChannel.IN_APP)
                : request.getChannels();
        channels.forEach(channel -> send(channel, request, response));

        log.info("Created notification: id={}, userId={}, code={}",
                saved.getId(), saved.getUserId(), saved.getCode());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        return PageResponse.from(notificationRepository.findByUserId(userId, pageable)
                .map(notificationMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .forEach(notification -> {
                    notification.setIsRead(true);
                    notification.setReadAt(LocalDateTime.now());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private void send(NotificationChannel channel, NotificationRequest request, NotificationResponse response) {
        if (channel == NotificationChannel.IN_APP) {
            messagingTemplate.convertAndSend("/topic/users/" + request.getUserId(), response);
        }
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            log.warn("No sender configured for notification channel {}", channel);
            return;
        }
        sender.send(request);
    }
}
