package com.project.booking.moderation.service.impl;

import com.project.booking.entity.Booking;
import com.project.booking.client.FieldManagementClient;
import com.project.booking.moderation.dto.*;
import com.project.booking.moderation.entity.*;
import com.project.booking.moderation.enums.PaymentDisputeStatus;
import com.project.booking.moderation.kafka.ModerationEventPublisher;
import com.project.booking.moderation.repository.*;
import com.project.booking.repository.UserProjectionRepository;
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
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final UserProjectionRepository userProjectionRepository;

    @Override
    @Transactional
    public FieldViolationResponse reportNoShow(UUID actorId, String actorRole, ReportNoShowRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
        UUID fieldId = booking.getSubField().getFieldId();
        requireManager(actorId, actorRole, booking, fieldId);
        preventSelfReport(actorId, booking.getClientId());
        if (booking.getStatus() == BookingStatus.REPORTED || noShowReportRepository.existsByBookingId(booking.getId())) {
            throw new BadRequestException("This booking has already been reported", "NO_SHOW_ALREADY_REPORTED");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed bookings can be reported as no-show");
        }

        noShowReportRepository.save(BookingNoShowReport.builder()
                .bookingId(booking.getId())
                .fieldId(fieldId)
                .reportedUserId(booking.getClientId())
                .ownerId(booking.getOwnerId())
                .build());
        booking.setStatus(BookingStatus.REPORTED);
        bookingRepository.save(booking);

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
            notifyUser(booking.getClientId(), "FIELD_BAN", "Bạn đã bị cấm đặt sân tại đây",
                    payload("fieldId", fieldId, "bookingId", booking.getId(), "violationCount", nextCount));
        } else {
            notifyUser(booking.getClientId(), "FIELD_VIOLATION_WARNING", "Cảnh báo vắng mặt sau khi đặt sân",
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
        PageResponse<FieldViolationResponse> response = PageResponse.from(violationRepository.findByFieldIdOrderByUpdatedAtDesc(fieldId, pageable)
                .map(this::toViolationResponse));
        return enrichViolationUsers(response);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FieldViolationResponse> getBannedClients(UUID actorId, String actorRole, UUID fieldId, Pageable pageable) {
        assertManagerCanAccessField(actorId, actorRole, fieldId);
        PageResponse<FieldViolationResponse> response = PageResponse.from(violationRepository.findByFieldIdAndBannedTrueOrderByBanDateDesc(fieldId, pageable)
                .map(this::toViolationResponse));
        return enrichViolationUsers(response);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingNoShowReportResponse> getNoShowReports(UUID actorId, String actorRole, UUID fieldId, Pageable pageable) {
        assertManagerCanAccessField(actorId, actorRole, fieldId);
        PageResponse<BookingNoShowReportResponse> response = PageResponse.from(noShowReportRepository.findByFieldIdOrderByCreatedAtDesc(fieldId, pageable)
                .map(this::toNoShowReportResponse));
        return enrichNoShowReportUsers(response);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ModerationAuditLogResponse> getAuditLogs(UUID actorId, String actorRole, UUID fieldId, Pageable pageable) {
        assertManagerCanAccessField(actorId, actorRole, fieldId);
        PageResponse<ModerationAuditLogResponse> response = PageResponse.from(auditLogRepository.findByFieldIdOrderByCreatedAtDesc(fieldId, pageable)
                .map(this::toAuditLogResponse));
        return enrichAuditLogUsers(response);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ModerationAuditLogResponse> getUserAuditLogs(UUID userId, Pageable pageable) {
        PageResponse<ModerationAuditLogResponse> response = PageResponse.from(auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toAuditLogResponse));
        return enrichAuditLogUsers(response);
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
            notifyUser(userId, "FIELD_BAN", "Bạn bị cấm đặt sân tại địa điểm này",
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
                .orElseThrow(() -> new NotFoundException("Field violation not found", "RESOURCE_NOT_FOUND"));
        violation.setBanned(false);
        violation.setBanDate(null);
        audit(actorId, userId, fieldId, "FIELD_UNBAN", "manual field manager unban");
        clearLocalPlatformBanIfBelowThreshold(userId);
        notifyUser(userId, "FIELD_UNBAN", "Lệnh cấm đặt sân đã được gỡ bỏ", payload("fieldId", fieldId));
        return toViolationResponse(violation);
    }

    @Override
    @Transactional
    public ModerationResetResponse resetPlatformBan(UUID actorId, UUID userId) {
        int platformBanRecordsCleared = platformBanRepository.deleteByUserId(userId);
        int fieldViolationRecordsReset = violationRepository.resetByUserId(userId);
        audit(actorId, userId, null, "PLATFORM_UNBAN_RESET", "platform ban and field violations reset by admin");
        notifyUser(userId, "PLATFORM_UNBAN", "Tài khoản của bạn đã được gỡ cấm", payload("resetBy", actorId));
        return ModerationResetResponse.builder()
                .userId(userId)
                .platformBanRecordsCleared(platformBanRecordsCleared)
                .fieldViolationRecordsReset(fieldViolationRecordsReset)
                .build();
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
                clearLocalPlatformBanIfBelowThreshold(violation.getUserId());
                notifyUser(violation.getUserId(), "FIELD_UNBAN", "Lệnh cấm đặt sân đã được tự động gỡ bỏ",
                        payload("fieldId", violation.getFieldId(), "violationCount", nextCount));
            }
        }
        return changed;
    }

    @Override
    public void ensureCanBook(UUID userId, UUID fieldId) {
        ensurePlatformAllowed(userId);
        if (violationRepository.existsByUserIdAndFieldIdAndBannedTrue(userId, fieldId)) {
            throw new ForbiddenException("You are banned from booking this field", "USER_FIELD_BANNED");
        }
    }

    @Override
    public void ensurePlatformAllowed(UUID userId) {
        if (platformBanRepository.existsByUserId(userId)) {
            throw new ForbiddenException("Your account is banned from booking and community features", "USER_PLATFORM_BANNED");
        }
    }

    @Override
    @Transactional
    public PaymentDisputeReportResponse createPaymentDispute(UUID ownerId, CreatePaymentDisputeReportRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
        requireOwner(ownerId, booking);
        preventSelfReport(ownerId, booking.getClientId());
        if (booking.getStatus() == BookingStatus.REPORTED
                || noShowReportRepository.existsByBookingId(booking.getId())
                || disputeRepository.existsByBookingId(booking.getId())) {
            throw new BadRequestException("A payment dispute already exists for this booking", "PAYMENT_DISPUTE_ALREADY_REPORTED");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed bookings can be reported for payment disputes");
        }
        PaymentDisputeReport report = PaymentDisputeReport.builder()
                .bookingId(booking.getId())
                .fieldId(booking.getSubField().getFieldId())
                .reportedUserId(booking.getClientId())
                .ownerId(ownerId)
                .description(request.getDescription())
                .imageUrls(request.getImageUrls() == null ? List.of() : request.getImageUrls())
                .status(PaymentDisputeStatus.PENDING)
                .build();
        PaymentDisputeReport saved = disputeRepository.save(report);
        booking.setStatus(BookingStatus.REPORTED);
        bookingRepository.save(booking);
        audit(ownerId, booking.getClientId(), saved.getFieldId(), "PAYMENT_DISPUTE_SUBMITTED", "reportId=" + saved.getId());
        notifyUser(ownerId, "PAYMENT_DISPUTE_SUBMITTED", "Báo cáo tranh chấp thanh toán đã được gửi",
                payload("reportId", saved.getId(), "bookingId", booking.getId()));
        return enrichDisputeUser(toDisputeResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentDisputeReportResponse> getOwnerDisputes(UUID ownerId, Pageable pageable) {
        return enrichDisputeUsers(PageResponse.from(disputeRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable).map(this::toDisputeResponse)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentDisputeReportResponse> getAdminDisputes(PaymentDisputeStatus status, Collection<UUID> fieldIds, Pageable pageable) {
        boolean hasFieldFilter = fieldIds != null && !fieldIds.isEmpty();
        return enrichDisputes(PageResponse.from(
                (status != null && hasFieldFilter
                        ? disputeRepository.findByStatusAndFieldIdInOrderByCreatedAtDesc(status, fieldIds, pageable)
                        : status != null
                            ? disputeRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                            : hasFieldFilter
                                ? disputeRepository.findByFieldIdInOrderByCreatedAtDesc(fieldIds, pageable)
                                : disputeRepository.findAllByOrderByCreatedAtDesc(pageable))
                        .map(this::toDisputeResponse)));
    }

    @Override
    @Transactional
    public PaymentDisputeReportResponse reviewPaymentDispute(UUID adminId, UUID reportId, ReviewPaymentDisputeRequest request) {
        PaymentDisputeReport report = disputeRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Payment dispute report not found", "PAYMENT_DISPUTE_NOT_FOUND"));
        if (report.getStatus() != PaymentDisputeStatus.PENDING) {
            throw new BadRequestException("This report has already been reviewed", "PAYMENT_DISPUTE_ALREADY_REVIEWED");
        }
        report.setStatus(Boolean.TRUE.equals(request.getApproved()) ? PaymentDisputeStatus.APPROVED : PaymentDisputeStatus.REJECTED);
        report.setAdminNote(request.getAdminNote());
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedBy(adminId);
        String code = report.getStatus() == PaymentDisputeStatus.APPROVED ? "PAYMENT_DISPUTE_APPROVED" : "PAYMENT_DISPUTE_REJECTED";
        notifyUser(report.getOwnerId(), code, "Kết quả xem xét tranh chấp thanh toán", payload("reportId", report.getId(), "status", report.getStatus()));
        notifyUser(report.getReportedUserId(), code, "Kết quả xem xét tranh chấp thanh toán", payload("reportId", report.getId(), "status", report.getStatus()));
        if (report.getStatus() == PaymentDisputeStatus.APPROVED) {
            createLocalPlatformBan(report.getReportedUserId(), "Approved payment dispute report " + report.getId());
            publisher.publishPlatformBanRequested(new PlatformBanRequestedEvent(
                    report.getReportedUserId(), "Approved payment dispute report " + report.getId(), adminId,
                    "PAYMENT_DISPUTE", Instant.now()));
            notifyUser(report.getReportedUserId(), "PLATFORM_BAN", "Tài khoản của bạn đã bị cấm vĩnh viễn",
                    payload("reportId", report.getId(), "reason", "PAYMENT_DISPUTE_APPROVED"));
        }
        audit(adminId, report.getReportedUserId(), report.getFieldId(), "PAYMENT_DISPUTE_" + report.getStatus(), request.getAdminNote());
        return enrichDisputeUser(toDisputeResponse(report));
    }

    private void requireOwner(UUID ownerId, Booking booking) {
        if (!ownerId.equals(booking.getOwnerId())) {
            throw new ForbiddenException("Only the owner of this field can perform this action");
        }
    }

    private void preventSelfReport(UUID actorId, UUID reportedUserId) {
        if (actorId.equals(reportedUserId)) {
            throw new BadRequestException("You cannot report yourself");
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
            notifyUser(userId, "PLATFORM_BAN", "Bạn đã bị cấm đặt sân vĩnh viễn. Vui lòng liên hệ bộ phận quản trị nếu muốn được mở tài khoản lại.",
                    payload("reason", "FIELD_VIOLATION_THRESHOLD"));
        }
    }

    private void clearLocalPlatformBanIfBelowThreshold(UUID userId) {
        if (violationRepository.countByUserIdAndBannedTrue(userId) < PLATFORM_BAN_FIELD_THRESHOLD) {
            platformBanRepository.deleteByUserId(userId);
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

    private BookingNoShowReportResponse toNoShowReportResponse(BookingNoShowReport report) {
        return BookingNoShowReportResponse.builder()
                .id(report.getId())
                .bookingId(report.getBookingId())
                .fieldId(report.getFieldId())
                .reportedUserId(report.getReportedUserId())
                .ownerId(report.getOwnerId())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private ModerationAuditLogResponse toAuditLogResponse(ModerationAuditLog log) {
        return ModerationAuditLogResponse.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .targetUserId(log.getTargetUserId())
                .fieldId(log.getFieldId())
                .action(log.getAction())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }

    private PageResponse<FieldViolationResponse> enrichViolationUsers(PageResponse<FieldViolationResponse> response) {
        List<FieldViolationResponse> violations = response.getContent();
        if (violations == null || violations.isEmpty()) {
            return response;
        }

        Set<UUID> userIds = violations.stream()
                .map(FieldViolationResponse::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserProjectionRepository.UserContactView> usersById = userProjectionRepository.findContactByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserProjectionRepository.UserContactView::getUserId, contact -> contact));

        violations.forEach(violation -> {
            UserProjectionRepository.UserContactView user = usersById.get(violation.getUserId());
            if (user != null) {
                violation.setUsername(displayUsername(user.getUsername(), user.getUserId()));
                violation.setPhoneNumber(user.getPhoneNumber());
            }
        });
        return response;
    }

    private PageResponse<BookingNoShowReportResponse> enrichNoShowReportUsers(PageResponse<BookingNoShowReportResponse> response) {
        List<BookingNoShowReportResponse> reports = response.getContent();
        if (reports == null || reports.isEmpty()) {
            return response;
        }

        Set<UUID> userIds = reports.stream()
                .map(BookingNoShowReportResponse::getReportedUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserProjectionRepository.UserContactView> usersById = userProjectionRepository.findContactByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserProjectionRepository.UserContactView::getUserId, contact -> contact));

        reports.forEach(report -> {
            UserProjectionRepository.UserContactView user = usersById.get(report.getReportedUserId());
            if (user != null) {
                report.setReportedUsername(displayUsername(user.getUsername(), user.getUserId()));
                report.setReportedPhoneNumber(user.getPhoneNumber());
            }
        });
        return response;
    }

    private PageResponse<ModerationAuditLogResponse> enrichAuditLogUsers(PageResponse<ModerationAuditLogResponse> response) {
        List<ModerationAuditLogResponse> logs = response.getContent();
        if (logs == null || logs.isEmpty()) {
            return response;
        }

        Set<UUID> userIds = logs.stream()
                .map(ModerationAuditLogResponse::getTargetUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return response;
        }
        Map<UUID, UserProjectionRepository.UserContactView> usersById = userProjectionRepository.findContactByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserProjectionRepository.UserContactView::getUserId, contact -> contact));

        logs.forEach(log -> {
            UserProjectionRepository.UserContactView user = usersById.get(log.getTargetUserId());
            if (user != null) {
                log.setTargetUsername(displayUsername(user.getUsername(), user.getUserId()));
                log.setTargetPhoneNumber(user.getPhoneNumber());
            }
        });
        return response;
    }

    private PageResponse<PaymentDisputeReportResponse> enrichDisputeUsers(PageResponse<PaymentDisputeReportResponse> response) {
        List<PaymentDisputeReportResponse> reports = response.getContent();
        if (reports == null || reports.isEmpty()) {
            return response;
        }

        Set<UUID> userIds = reports.stream()
                .map(PaymentDisputeReportResponse::getReportedUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserProjectionRepository.UserContactView> usersById = userProjectionRepository.findContactByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserProjectionRepository.UserContactView::getUserId, contact -> contact));

        reports.forEach(report -> enrichDisputeUser(report, usersById.get(report.getReportedUserId())));
        return response;
    }

    private PaymentDisputeReportResponse enrichDisputeUser(PaymentDisputeReportResponse response) {
        userProjectionRepository.findContactByUserIdIn(Set.of(response.getReportedUserId())).stream()
                .findFirst()
                .ifPresent(user -> enrichDisputeUser(response, user));
        return response;
    }

    private void enrichDisputeUser(PaymentDisputeReportResponse response, UserProjectionRepository.UserContactView user) {
        if (user == null) {
            return;
        }
        response.setReportedUsername(displayUsername(user.getUsername(), user.getUserId()));
        response.setReportedPhoneNumber(user.getPhoneNumber());
        response.setReportedUserStatus(user.getStatus());
    }

    private PageResponse<PaymentDisputeReportResponse> enrichDisputes(PageResponse<PaymentDisputeReportResponse> response) {
        enrichDisputeUsers(response);
        enrichDisputeBookings(response);
        return response;
    }

    private void enrichDisputeBookings(PageResponse<PaymentDisputeReportResponse> response) {
        List<PaymentDisputeReportResponse> reports = response.getContent();
        if (reports == null || reports.isEmpty()) {
            return;
        }

        Set<UUID> bookingIds = reports.stream()
                .map(PaymentDisputeReportResponse::getBookingId)
                .collect(Collectors.toSet());
        Map<UUID, Booking> bookingsById = bookingRepository.findAllById(bookingIds).stream()
                .collect(Collectors.toMap(Booking::getId, booking -> booking));

        reports.forEach(report -> {
            Booking booking = bookingsById.get(report.getBookingId());
            if (booking == null) {
                return;
            }
            report.setBookingCode(booking.getBookingCode());
            report.setSubFieldId(booking.getSubFieldId());
            report.setBookingDate(booking.getBookingDate());
            report.setStartDateTime(booking.getStartDateTime());
            report.setEndDateTime(booking.getEndDateTime());
            report.setStartTime(booking.getStartTime());
            report.setEndTime(booking.getEndTime());
            report.setBookingPrice(booking.getBookingPrice());
            report.setPlatformBookingFee(booking.getPlatformBookingFee());
            report.setBookingStatus(booking.getStatus());
            report.setBookingPaymentStatus(booking.getPaymentStatus());
            if (booking.getSubField() != null) {
                report.setFieldName(booking.getSubField().getFieldName());
                report.setSubFieldName(booking.getSubField().getName());
            }
        });
    }

    private String displayUsername(String username, UUID userId) {
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        String value = userId.toString();
        return "User " + value.substring(value.length() - 4);
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
