package com.project.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.booking.config.SecurityConfig;
import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.request.UpsertMatchResultRequest;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.UnavailableSlotResponse;
import com.project.booking.enums.WinningTeam;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.service.BookingConfigService;
import com.project.booking.service.BookingService;
import com.project.booking.service.MatchResultService;
import com.project.booking.repository.BookingRepository;
import com.project.common.constants.GlobalConstants;
import com.project.common.dto.PageResponse;
import com.project.common.enums.BookingStatus;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.common.security.HeaderAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class, GlobalExceptionHandler.class})
class BookingControllerApiTest {

    private static final UUID USER_ID = UUID.fromString("b1e1c606-6b76-4154-af38-7dda890395ce");
    private static final UUID BOOKING_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SUB_FIELD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String INTERNAL_SECRET = "dev-internal-gateway-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private BookingConfigService bookingConfigService;

    @MockitoBean
    private MatchResultService matchResultService;

    @MockitoBean
    private BookingRepository bookingRepository;

    @Test
    void internalConflictCheckReturnsRepositoryResult() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(2);
        when(bookingRepository.existsBySubFieldIdInAndBookingDateBetweenAndStatusIn(
                org.mockito.ArgumentMatchers.anyCollection(), eq(startDate), eq(endDate),
                org.mockito.ArgumentMatchers.anyCollection())).thenReturn(true);

        mockMvc.perform(get("/api/v1/bookings/internal/conflicts")
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET)
                        .param("subFieldIds", SUB_FIELD_ID.toString())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void createBookingWithClientHeaderReturnsCreated() throws Exception {
        CreateBookingRequest request = bookingRequest();
        BookingResponse response = bookingResponse();
        when(bookingService.createBooking(eq(USER_ID), org.mockito.ArgumentMatchers.any(CreateBookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .headers(clientHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking created successfully"))
                .andExpect(jsonPath("$.data.id").value(BOOKING_ID.toString()));

        ArgumentCaptor<CreateBookingRequest> captor = ArgumentCaptor.forClass(CreateBookingRequest.class);
        verify(bookingService).createBooking(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getSubFieldId()).isEqualTo(SUB_FIELD_ID);
    }

    @Test
    void createBookingRejectsOwnerRole() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this operation"));
    }

    @Test
    void createBookingRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .headers(clientHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createBookingReturnsConflictWhenServiceFindsOverlap() throws Exception {
        when(bookingService.createBooking(eq(USER_ID), org.mockito.ArgumentMatchers.any(CreateBookingRequest.class)))
                .thenThrow(new BookingConflictException("The selected time slot is no longer available."));

        mockMvc.perform(post("/api/v1/bookings")
                        .headers(clientHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_CONFLICT"));
    }

    @Test
    void createBookingReturnsBadRequestForBusinessValidationErrors() throws Exception {
        when(bookingService.createBooking(eq(USER_ID), org.mockito.ArgumentMatchers.any(CreateBookingRequest.class)))
                .thenThrow(new BadRequestException("SubField is closed on the selected booking date"));

        mockMvc.perform(post("/api/v1/bookings")
                        .headers(clientHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("SubField is closed on the selected booking date"));
    }

    @Test
    void cancelBookingWithClientHeaderReturnsOk() throws Exception {
        CancelBookingRequest request = CancelBookingRequest.builder().bookingId(BOOKING_ID).reason("Change").build();
        when(bookingService.cancelBooking(eq(USER_ID), org.mockito.ArgumentMatchers.any(CancelBookingRequest.class)))
                .thenReturn(bookingResponse());

        mockMvc.perform(patch("/api/v1/bookings/cancel")
                        .headers(clientHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Booking cancelled successfully"));
    }

    @Test
    void ownerCancelBookingWithOwnerHeaderReturnsOk() throws Exception {
        CancelBookingRequest request = CancelBookingRequest.builder().bookingId(BOOKING_ID).reason("Maintenance").build();
        when(bookingService.cancelBookingByOwner(eq(USER_ID), org.mockito.ArgumentMatchers.any(CancelBookingRequest.class)))
                .thenReturn(bookingResponse());

        mockMvc.perform(patch("/api/v1/bookings/owner/cancel")
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Booking cancelled successfully"));
    }

    @Test
    void getMyBookingsWithClientHeaderReturnsList() throws Exception {
        when(bookingService.getMyBookings(eq(USER_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(bookingPageResponse());

        mockMvc.perform(get("/api/v1/bookings/my").headers(clientHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(BOOKING_ID.toString()));
    }

    @Test
    void getOwnerBookingsWithOwnerHeaderReturnsList() throws Exception {
        when(bookingService.getOwnerBookings(eq(USER_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(bookingPageResponse());

        mockMvc.perform(get("/api/v1/bookings/owner")
                        .headers(ownerHeaders())
                        .param("bookingDate", LocalDate.now().plusDays(3).toString())
                        .param("subFieldId", SUB_FIELD_ID.toString())
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(BOOKING_ID.toString()));
    }

    @Test
    void ownerUpsertMatchResultReturnsBooking() throws Exception {
        UpsertMatchResultRequest request = new UpsertMatchResultRequest();
        request.setWinningTeam(WinningTeam.TEAM_A);
        request.setTeamAPercentage(70);
        request.setTeamBPercentage(30);
        when(matchResultService.upsert(eq(USER_ID), eq(BOOKING_ID), org.mockito.ArgumentMatchers.any(UpsertMatchResultRequest.class)))
                .thenReturn(bookingResponse());

        mockMvc.perform(put("/api/v1/bookings/owner/{bookingId}/match-result", BOOKING_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Match result saved successfully"));
    }

    @Test
    void getBookingByIdWithClientHeaderReturnsBooking() throws Exception {
        when(bookingService.getBookingById(BOOKING_ID, USER_ID)).thenReturn(bookingResponse());

        mockMvc.perform(get("/api/v1/bookings/{bookingId}", BOOKING_ID).headers(clientHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(BOOKING_ID.toString()));
    }

    @Test
    void getAvailabilityIsPublicAndReturnsSlots() throws Exception {
        LocalDate date = LocalDate.now().plusDays(3);
        when(bookingService.getAvailability(SUB_FIELD_ID, date)).thenReturn(AvailabilityResponse.builder()
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(23, 0))
                .unavailableSlots(List.of(UnavailableSlotResponse.builder()
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(10, 0))
                        .build()))
                .build());

        mockMvc.perform(get("/api/v1/bookings/availability")
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET)
                        .param("subFieldId", SUB_FIELD_ID.toString())
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openTime").value("06:00:00"))
                .andExpect(jsonPath("$.data.unavailableSlots[0].startTime").value("09:00:00"));
    }

    @Test
    void protectedEndpointWithoutHeadersIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/my"))
                .andExpect(status().isForbidden());
    }

    private CreateBookingRequest bookingRequest() {
        return CreateBookingRequest.builder()
                .subFieldId(SUB_FIELD_ID)
                .bookingDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(8, 0))
                .durationMinutes(60)
                .note("API test")
                .build();
    }

    private BookingResponse bookingResponse() {
        return BookingResponse.builder()
                .id(BOOKING_ID)
                .bookingCode("BK-TEST")
                .clientId(USER_ID)
                .subFieldId(SUB_FIELD_ID)
                .ownerId(USER_ID)
                .bookingDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .durationMinutes(60)
                .pricePerHour(new BigDecimal("100000"))
                .totalAmount(new BigDecimal("100000"))
                .status(BookingStatus.PENDING)
                .build();
    }

    private PageResponse<BookingResponse> bookingPageResponse() {
        return PageResponse.<BookingResponse>builder()
                .content(List.of(bookingResponse()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();
    }

    private org.springframework.http.HttpHeaders clientHeaders() {
        return headers("CLIENT");
    }

    private org.springframework.http.HttpHeaders ownerHeaders() {
        return headers("OWNER");
    }

    private org.springframework.http.HttpHeaders headers(String role) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET);
        headers.add(GlobalConstants.HEADER_USER_ID, USER_ID.toString());
        headers.add(GlobalConstants.HEADER_USER_ROLE, role);
        headers.add(GlobalConstants.HEADER_USER_EMAIL, "api-test@example.com");
        return headers;
    }
}
