package com.project.booking.community.service;

import java.util.UUID;

public interface CommunityPostMaintenanceService {
    void cancelOpenPostForBooking(UUID bookingId);
    int closeStartedOpenPosts();
}
