package com.project.booking.moderation.service.impl;

import com.project.booking.entity.Booking;
import com.project.booking.client.FieldManagementClient;
import com.project.booking.moderation.dto.*;
import com.project.booking.moderation.entity.*;
import com.project.booking.moderation.enums.PaymentDisputeStatus;
import com.project.booking.moderation.kafka.ModerationEventPublisher;
import com.project.booking.moderation.repository.*;
import com.project.booking.moderation.service.BookingModerationService;
import com.project.booking.repository.BookingRepository;
import com.project.common.dto.PageResponse;
import com.project.common.enums.BookingStatus;
import com.project.common.events.notification.ModerationNotificationEvent;
import com.project.common.events.notification.PlatformBanRequestedEvent;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingModerationServiceImpl implements BookingModerationService {
    private static final int FIELD_BAN_THRESHOLD = 3;
    private static final int PLATFORM_BAN_FIELD_THRESHOLD = 2;

    private final BookingRepository bookingRepository;
    private final FieldViolationRepository violationRepository;
    private final BookingNoShowReportRepository noShowReportRepository;
    private final PaymentDisputeReportRepository disputeRepository;
    private final ModerationAuditLogRepository auditLogRepository;
    private final PlatformBanRepository platformBanRepository;
    private final ModerationEventPublisher publisher;
    private final FieldManagementClient fieldManagementClient;

    @Override
    @Transactional
    public FieldViolationResponse reportNoShow(UUID actorId, String actorRole, ReportNoShowRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        UUID fieldId = booking.getSubField().getFieldId();
        requireManager(actorId, actorRole, booking, fieldId);
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed bookings can be reported as no-show");
        }
        if (noShowReportRepository.existsByBookingId(booking.getId())) {
            throw new BadRequestException("This booking has already been reported");
        }

        noShowReportRepository.save(BookingNoShowReport.builder()
                .bookingId(booking.getId())
                .fieldId(fieldId)
                .reportedUserId(booking.getClientId())
                .ownerId(booking.getOwnerId())
                .build());

        FieldViolation violation = violationRepository.findForUpdateByUserIdAndFieldId(booking.getClientId(), fieldId)
                .orElseGet(() -> FieldViolation.builder()
                        .userId(booking.getClientId())
                        .fieldId(fieldId)
                        .violationCount(0)
                        .banned(false)
                        .build());
        int nextCount = (violation.getViolationCount() == null ? 0 : violation.getViolationCount()) + 1;
        violation.setViolationCount(nextCount);
        violation.setLastViolationDate(LocalDateTime.now());
        if (nextCount >= FIELD_BAN_THRESHOLD && !Boolean.TRUE.equals(violation.getBanned())) {
            violation.setBanned(true);
            violation.setBanDate(LocalDateTime.now());
            notifyUser(booking.getClientId(), "FIELD_BAN", "Ban bi cam dat san tai dia diem nay",
                    payload("fieldId", fieldId, "bookingId", booking.getId(), "violationCount", nextCount));
        } else {
            notifyUser(booking.getClientId(), "FIELD_VIOLATION_WARNING", "Canh bao vang mat khi dat san",
                    payload("fieldId", fieldId, "bookingId", booking.getId(), "violationCount", nextCount));
        }
        FieldViolation saved = violationRepository.save(violation);
        audit(actorId, booking.getClientId(), fieldId, "NO_SHOW_REPORTED", "bookingId=" + booking.getId());
        enforcePlatformBanIfNeeded(booking.getClientId(), actorId);
        return toViolationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FieldViolationResponse> getUserFieldViolations(UUID userId, Pageable pageable) {
        return PageResponse.from(violationRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(this::toViolationResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FieldViolationResponse> getViolations(UUID actorId, String actorRole, UUID fieldId, Pageable pageable) {
        assertManagerCanAccessField(actorId, actorRole, fieldId);
        return PageResponse.from(violationRepository.findByFieldIdOrderByUpdatedAtDesc(fieldId, pageable)
                .map(this::toViolationResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FieldViolationResponse> getBannedClients(UUID actorId, String actorRole, UUID fieldId, Pageable pageable) {
        assertManagerCanAccessField(actorId, actorRole, fieldId);
        return PageResponse.from(violationRepository.findByFieldIdAndBannedTrueOrderByBanDateDesc(fieldId, pageable)
                .map(this::toViolationResponse));
    }

    @Override
    @Transactional
    public FieldViolationResponse ban(UUID actorId, String actorRole, UUID fieldId, UUID userId) {
        assertManagerCanAccessField(actorId, actorRole, fieldId);
        FieldViolation violation = violationRepository.findForUpdateByUserIdAndFieldId(userId, fieldId)
                .orElseGet(() -> FieldViolation.builder()
                        .userId(userId)
                        .fieldId(fieldId)
                        .violationCount(0)
                        .banned(false)
                        .build());
        if (!Boolean.TRUE.equals(violation.getBanned())) {
            violation.setBanned(true);
            violation.setBanDate(LocalDateTime.now());
            notifyUser(userId, "FIELD_BAN", "Ban bi cam dat san tai dia diem nay",
                    payload("fieldId", fieldId, "violationCount", violation.getViolationCount()));
        }
        FieldViolation saved = violationRepository.save(violation);
        audit(actorId, userId, fieldId, "FIELD_BAN", "manual field manager ban");
        enforcePlatformBanIfNeeded(userId, actorId);
        return toViolationResponse(saved);
    }

    @Override
    @Transactional
    public FieldViolationResponse unban(UUID actorId, String actorRole, UUID fieldId, UUID userId) {
        assertManagerCanAccessField(actorId, actorRole, fieldId);
        FieldViolation violation = violationRepository.findForUpdateByUserIdAndFieldId(userId, fieldId)
                .orElseThrow(() -> new NotFoundException("Field violation not found"));
        violation.setBanned(false);
        violation.setBanDate(null);
        audit(actorId, userId, fieldId, "FIELD_UNBAN", "manual field manager unban");
        notifyUser(userId, "FIELD_UNBAN", "Lenh cam dat san da duoc go bo", payload("fieldId", fieldId));
        return toViolationResponse(violation);
    }

    @Override
    @Transactional
    public int recoverMonthlyViolations() {
        int changed = 0;
        for (FieldViolation violation : violationRepository.findAll()) {
            int nextCount = Math.max(0, (violation.getViolationCount() == null ? 0 : violation.getViolationCount()) - 1);
            if (nextCount != violation.getViolationCount()) {
                violation.setViolationCount(nextCount);
                changed++;
            }
            if (nextCount < FIELD_BAN_THRESHOLD && Boolean.TRUE.equals(violation.getBanned())) {
                violation.setBanned(false);
                violation.setBanDate(null);
                notifyUser(violation.getUserId(), "FIELD_UNBAN", "Lenh cam dat san da duoc tu dong go bo",
                        payload("fieldId", violation.getFieldId(), "violationCount", nextCount));
            }
        }
        return changed;
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureCanBook(UUID userId, UUID fieldId) {
        ensurePlatformAllowed(userId);
        if (violationRepository.existsByUserIdAndFieldIdAndBannedTrue(userId, fieldId)) {
            throw new ForbiddenException("You are banned from booking this field");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void ensurePlatformAllowed(UUID userId) {
        if (platformBanRepository.existsByUserId(userId)) {
            throw new ForbiddenException("Your account is banned from booking and community features");
        }
        if (violationRepository.countByUserIdAndBannedTrue(userId) >= PLATFORM_BAN_FIELD_THRESHOLD) {
            throw new ForbiddenException("Your account is banned from booking and community features");
        }
    }

    @Override
    @Transactional
    public PaymentDisputeReportResponse createPaymentDispute(UUID ownerId, CreatePaymentDisputeReportRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        requireOwner(ownerId, booking);
        if (disputeRepository.existsByBookingIdAndOwnerId(booking.getId(), ownerId)) {
            throw new BadRequestException("A payment dispute already exists for this booking");
        }
        PaymentDisputeReport report = PaymentDisputeReport.builder()
                .bookingId(booking.getId())
                .fieldId(booking.getSubField().getFieldId())
                .reportedUserId(booking.getClientId())
                .ownerId(ownerId)
                .description(request.getDescription())
                .imageUrls(request.getImageUrls())
                .status(PaymentDisputeStatus.PENDING)
                .build();
        PaymentDisputeReport saved = disputeRepository.save(report);
        audit(ownerId, booking.getClientId(), saved.getFieldId(), "PAYMENT_DISPUTE_SUBMITTED", "reportId=" + saved.getId());
        notifyUser(ownerId, "PAYMENT_DISPUTE_SUBMITTED", "Bao cao tranh chap thanh toan da duoc gui",
                payload("reportId", saved.getId(), "bookingId", booking.getId()));
        return toDisputeResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentDisputeReportResponse> getOwnerDisputes(UUID ownerId, Pageable pageable) {
        return PageResponse.from(disputeRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable).map(this::toDisputeResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentDisputeReportResponse> getAdminDisputes(PaymentDisputeStatus status, Pageable pageable) {
        return PageResponse.from((status == null
                ? disputeRepository.findAllByOrderByCreatedAtDesc(pageable)
                : disputeRepository.findByStatusOrderByCreatedAtDesc(status, pageable)).map(this::toDisputeResponse));
    }

    @Override
    @Transactional
    public PaymentDisputeReportResponse reviewPaymentDispute(UUID adminId, UUID reportId, ReviewPaymentDisputeRequest request) {
        PaymentDisputeReport report = disputeRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Payment dispute report not found"));
        if (report.getStatus() != PaymentDisputeStatus.PENDING) {
            throw new BadRequestException("This report has already been reviewed");
        }
        report.setStatus(Boolean.TRUE.equals(request.getApproved()) ? PaymentDisputeStatus.APPROVED : PaymentDisputeStatus.REJECTED);
        report.setAdminNote(request.getAdminNote());
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedBy(adminId);
        String code = report.getStatus() == PaymentDisputeStatus.APPROVED ? "PAYMENT_DISPUTE_APPROVED" : "PAYMENT_DISPUTE_REJECTED";
        notifyUser(report.getOwnerId(), code, "Ket qua xem xet tranh chap thanh toan", payload("reportId", report.getId(), "status", report.getStatus()));
        notifyUser(report.getReportedUserId(), code, "Ket qua xem xet tranh chap thanh toan", payload("reportId", report.getId(), "status", report.getStatus()));
        if (report.getStatus() == PaymentDisputeStatus.APPROVED) {
            createLocalPlatformBan(report.getReportedUserId(), "Approved payment dispute report " + report.getId());
            publisher.publishPlatformBanRequested(new PlatformBanRequestedEvent(
                    report.getReportedUserId(), "Approved payment dispute report " + report.getId(), adminId,
                    "PAYMENT_DISPUTE", Instant.now()));
            notifyUser(report.getReportedUserId(), "PLATFORM_BAN", "Tai khoan cua ban da bi cam vinh vien",
                    payload("reportId", report.getId(), "reason", "PAYMENT_DISPUTE_APPROVED"));
        }
        audit(adminId, report.getReportedUserId(), report.getFieldId(), "PAYMENT_DISPUTE_" + report.getStatus(), request.getAdminNote());
        return toDisputeResponse(report);
    }

    private void requireOwner(UUID ownerId, Booking booking) {
        if (!ownerId.equals(booking.getOwnerId())) {
            throw new ForbiddenException("Only the owner of this field can perform this action");
        }
    }

    private void requireManager(UUID actorId, String actorRole, Booking booking, UUID fieldId) {
        if ("OWNER".equals(actorRole) && actorId.equals(booking.getOwnerId())) {
            return;
        }
        if ("EMPLOYEE".equals(actorRole) && fieldManagementClient.canManageField(actorId, actorRole, fieldId)) {
            return;
        }
        throw new ForbiddenException("Only a field owner or assigned employee can perform this action");
    }

    private void assertManagerCanAccessField(UUID actorId, String actorRole, UUID fieldId) {
        if ("OWNER".equals(actorRole) && bookingRepository.existsByOwnerIdAndSubFieldFieldId(actorId, fieldId)) {
            return;
        }
        if ("EMPLOYEE".equals(actorRole) && fieldManagementClient.canManageField(actorId, actorRole, fieldId)) {
            return;
        }
        throw new ForbiddenException("Only a field owner or assigned employee can perform this action");
    }

    private void enforcePlatformBanIfNeeded(UUID userId, UUID actorId) {
        if (violationRepository.countByUserIdAndBannedTrue(userId) >= PLATFORM_BAN_FIELD_THRESHOLD) {
            createLocalPlatformBan(userId, "Banned from two or more fields");
            publisher.publishPlatformBanRequested(new PlatformBanRequestedEvent(
                    userId, "Banned from two or more fields", actorId, "FIELD_VIOLATION", Instant.now()));
            notifyUser(userId, "PLATFORM_BAN", "Tai khoan cua ban da bi cam vinh vien",
                    payload("reason", "FIELD_VIOLATION_THRESHOLD"));
        }
    }

    private void notifyUser(UUID userId, String code, String title, Map<String, Object> payload) {
        publisher.publishNotification(new ModerationNotificationEvent(userId, null, code, title, payload, Instant.now()));
    }

    private void createLocalPlatformBan(UUID userId, String reason) {
        if (platformBanRepository.existsByUserId(userId)) {
            return;
        }
        platformBanRepository.save(PlatformBan.builder()
                .userId(userId)
                .reason(reason)
                .bannedAt(LocalDateTime.now())
                .build());
    }

    private void audit(UUID actorId, UUID targetUserId, UUID fieldId, String action, String details) {
        auditLogRepository.save(ModerationAuditLog.builder()
                .actorId(actorId)
                .targetUserId(targetUserId)
                .fieldId(fieldId)
                .action(action)
                .details(details)
                .build());
    }

    private Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            payload.put(String.valueOf(values[i]), values[i + 1]);
        }
        return payload;
    }

    private FieldViolationResponse toViolationResponse(FieldViolation violation) {
        return FieldViolationResponse.builder()
                .id(violation.getId())
                .userId(violation.getUserId())
                .fieldId(violation.getFieldId())
                .violationCount(violation.getViolationCount())
                .banned(violation.getBanned())
                .banDate(violation.getBanDate())
                .lastViolationDate(violation.getLastViolationDate())
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .build();
    }

    private PaymentDisputeReportResponse toDisputeResponse(PaymentDisputeReport report) {
        return PaymentDisputeReportResponse.builder()
                .id(report.getId())
                .bookingId(report.getBookingId())
                .fieldId(report.getFieldId())
                .reportedUserId(report.getReportedUserId())
                .ownerId(report.getOwnerId())
                .description(report.getDescription())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .imageUrls(report.getImageUrls())
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .reviewedBy(report.getReviewedBy())
                .build();
    }
}
