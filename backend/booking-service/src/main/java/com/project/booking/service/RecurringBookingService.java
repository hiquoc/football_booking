package com.project.booking.service;

import com.project.booking.dto.request.CreateRecurringBookingRequest;
import com.project.booking.dto.request.UpdateRecurringBookingRequest;
import com.project.booking.dto.response.RecurringBookingResponse;
import com.project.common.dto.PageResponse;
import com.project.common.enums.RecurringBookingStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RecurringBookingService {
    RecurringBookingResponse create(UUID userId, CreateRecurringBookingRequest request);
    RecurringBookingResponse update(UUID userId, UUID id, UpdateRecurringBookingRequest request);
    RecurringBookingResponse pause(UUID userId, UUID id);
    RecurringBookingResponse resume(UUID userId, UUID id);
    RecurringBookingResponse cancel(UUID userId, UUID id);
    RecurringBookingResponse ownerPause(UUID ownerId, UUID id);
    RecurringBookingResponse ownerResume(UUID ownerId, UUID id);
    RecurringBookingResponse ownerCancel(UUID ownerId, UUID id);
    RecurringBookingResponse adminPause(UUID id);
    RecurringBookingResponse adminResume(UUID id);
    RecurringBookingResponse adminCancel(UUID id);
    PageResponse<RecurringBookingResponse> getMine(UUID userId, RecurringBookingStatus status, Pageable pageable);
    PageResponse<RecurringBookingResponse> getForOwner(UUID ownerId, RecurringBookingStatus status, Pageable pageable);
    PageResponse<RecurringBookingResponse> getForAdmin(RecurringBookingStatus status, Pageable pageable);
    void processDue(LocalDateTime now);
}
