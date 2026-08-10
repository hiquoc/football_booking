package com.project.booking.moderation.service.impl;

import com.project.booking.client.FieldManagementClient;
import com.project.booking.entity.Booking;
import com.project.booking.entity.SubFieldProjection;
import com.project.booking.moderation.dto.FieldViolationResponse;
import com.project.booking.moderation.dto.CreatePaymentDisputeReportRequest;
import com.project.booking.moderation.dto.ReportNoShowRequest;
import com.project.booking.moderation.entity.FieldViolation;
import com.project.booking.moderation.kafka.ModerationEventPublisher;
import com.project.booking.moderation.repository.BookingNoShowReportRepository;
import com.project.booking.moderation.repository.FieldViolationRepository;
import com.project.booking.moderation.repository.ModerationAuditLogRepository;
import com.project.booking.moderation.repository.PaymentDisputeReportRepository;
import com.project.booking.moderation.repository.PlatformBanRepository;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.UserProjectionRepository;
import com.project.common.dto.PageResponse;
import com.project.common.enums.BookingStatus;
import com.project.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingModerationServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FieldViolationRepository violationRepository;

    @Mock
    private BookingNoShowReportRepository noShowReportRepository;

    @Mock
    private PaymentDisputeReportRepository disputeRepository;

    @Mock
    private ModerationAuditLogRepository auditLogRepository;

    @Mock
    private PlatformBanRepository platformBanRepository;

    @Mock
    private ModerationEventPublisher publisher;

    @Mock
    private FieldManagementClient fieldManagementClient;

    @Mock
    private UserProjectionRepository userProjectionRepository;

    @InjectMocks
    private BookingModerationServiceImpl service;

    @Test
    void reportNoShowRejectsSelfReport() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        Booking booking = booking(bookingId, userId, userId, fieldId, BookingStatus.COMPLETED);
        ReportNoShowRequest request = new ReportNoShowRequest();
        request.setBookingId(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(java.util.Optional.of(booking));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> service.reportNoShow(userId, "OWNER", request));

        assertEquals("You cannot report yourself", error.getMessage());
        verify(noShowReportRepository, never()).save(any());
        verify(violationRepository, never()).save(any());
    }

    @Test
    void createPaymentDisputeRejectsSelfReport() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        Booking booking = booking(bookingId, userId, userId, fieldId, BookingStatus.COMPLETED);
        CreatePaymentDisputeReportRequest request = new CreatePaymentDisputeReportRequest();
        request.setBookingId(bookingId);
        request.setDescription("Self dispute");

        when(bookingRepository.findById(bookingId)).thenReturn(java.util.Optional.of(booking));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> service.createPaymentDispute(userId, request));

        assertEquals("You cannot report yourself", error.getMessage());
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void getViolationsEnrichesPagedResultsWithBatchUserProjectionData() {
        UUID ownerId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        FieldViolation firstViolation = FieldViolation.builder()
                .id(UUID.randomUUID())
                .userId(firstUserId)
                .fieldId(fieldId)
                .violationCount(2)
                .banned(false)
                .lastViolationDate(LocalDateTime.now())
                .build();
        FieldViolation secondViolation = FieldViolation.builder()
                .id(UUID.randomUUID())
                .userId(secondUserId)
                .fieldId(fieldId)
                .violationCount(3)
                .banned(true)
                .lastViolationDate(LocalDateTime.now())
                .build();

        when(bookingRepository.existsByOwnerIdAndSubFieldFieldId(ownerId, fieldId)).thenReturn(true);
        when(violationRepository.findByFieldIdOrderByUpdatedAtDesc(fieldId, pageable))
                .thenReturn(new PageImpl<>(List.of(firstViolation, secondViolation), pageable, 2));
        when(userProjectionRepository.findContactByUserIdIn(any())).thenReturn(List.of(
                userContact(firstUserId, "Nguyen Van A", "0862470050"),
                userContact(secondUserId, "Tran Van B", "0912345678")));

        PageResponse<FieldViolationResponse> response = service.getViolations(ownerId, "OWNER", fieldId, pageable);

        assertEquals(2, response.getContent().size());
        assertEquals("Nguyen Van A", response.getContent().get(0).getUsername());
        assertEquals("0862470050", response.getContent().get(0).getPhoneNumber());
        assertEquals("Tran Van B", response.getContent().get(1).getUsername());
        assertEquals("0912345678", response.getContent().get(1).getPhoneNumber());

        verify(userProjectionRepository).findContactByUserIdIn(any());
        verify(violationRepository).findByFieldIdOrderByUpdatedAtDesc(eq(fieldId), eq(pageable));
    }

    private UserProjectionRepository.UserContactView userContact(UUID userId, String username, String phoneNumber) {
        return new UserProjectionRepository.UserContactView() {
            @Override
            public UUID getUserId() {
                return userId;
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getPhoneNumber() {
                return phoneNumber;
            }

            @Override
            public String getStatus() {
                return "ACTIVE";
            }
        };
    }

    private Booking booking(UUID bookingId, UUID clientId, UUID ownerId, UUID fieldId, BookingStatus status) {
        return Booking.builder()
                .id(bookingId)
                .clientId(clientId)
                .ownerId(ownerId)
                .status(status)
                .subField(SubFieldProjection.builder()
                        .fieldId(fieldId)
                        .ownerId(ownerId)
                        .build())
                .build();
    }
}
