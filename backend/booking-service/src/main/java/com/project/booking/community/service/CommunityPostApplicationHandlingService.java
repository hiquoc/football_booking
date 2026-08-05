package com.project.booking.community.service;

import java.util.UUID;

public interface CommunityPostApplicationHandlingService {
    void handlePostClosed(UUID postId, String notificationCode, String notificationTitle, String phase, int page);
}
