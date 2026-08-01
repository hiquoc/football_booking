package com.project.booking.service.impl;

import com.project.booking.client.FieldManagementClient;
import com.project.booking.dto.request.UpsertMatchResultRequest;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.entity.Booking;
import com.project.booking.entity.MatchResult;
import com.project.booking.enums.WinningTeam;
import com.project.booking.exception.BookingNotFoundException;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.MatchResultRepository;
import com.project.booking.service.MatchStatisticsAdjustmentService;
import com.project.booking.service.MatchResultService;
import com.project.common.enums.BookingStatus;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchResultServiceImpl implements MatchResultService {
    private final BookingRepository bookingRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchStatisticsAdjustmentService statisticsAdjustmentService;
    private final BookingMapper bookingMapper;
    private final FieldManagementClient fieldManagementClient;

    @Override
    @Transactional
    public BookingResponse upsert(UUID managerId, String managerRole, UUID bookingId, UpsertMatchResultRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        validateManagerCanSubmit(managerId, managerRole, booking);
        validateSplit(request);

        Optional<MatchResult> previous = matchResultRepository.findByBookingId(bookingId);
        WinningTeam previousResult = previous.map(matchResult -> normalize(matchResult.getWinningTeam())).orElse(null);
        WinningTeam nextResult = normalize(request.getResult());

        MatchResult result = previous.orElseGet(() -> MatchResult.builder()
                .bookingId(bookingId)
                .build());
        result.setWinningTeam(nextResult);
        result.setTeamAPercentage(request.getTeamAPercentage());
        result.setTeamBPercentage(request.getTeamBPercentage());
        result.setTeamAAmount(calculateAmount(booking.getSubFieldPrice(), request.getTeamAPercentage()));
        result.setTeamBAmount(calculateAmount(booking.getSubFieldPrice(), request.getTeamBPercentage()));
        result.setSubmittedBy(managerId);

        MatchResult saved = matchResultRepository.save(result);
        statisticsAdjustmentService.adjustForResultChange(booking, previousResult, saved.getWinningTeam());

        BookingResponse response = bookingMapper.toResponse(booking);
        response.setMatchResult(bookingMapper.toMatchResultResponse(saved));
        return response;
    }

    private void validateManagerCanSubmit(UUID managerId, String managerRole, Booking booking) {
        if (!"OWNER".equals(managerRole) || !booking.getOwnerId().equals(managerId)) {
            UUID fieldId = booking.getSubField() == null ? null : booking.getSubField().getFieldId();
            if (!"EMPLOYEE".equals(managerRole) || fieldId == null || !fieldManagementClient.canManageField(managerId, managerRole, fieldId)) {
                throw new UnauthorizedException("Only the field owner or assigned employee may submit this match result");
            }
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BadRequestException("Cancelled or expired bookings cannot have match results");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed bookings can have match results");
        }
        LocalDateTime finishedAt = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());
        if (finishedAt.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Booking has not finished yet");
        }
    }

    private void validateSplit(UpsertMatchResultRequest request) {
        if (request.getTeamAPercentage() + request.getTeamBPercentage() != 100) {
            throw new BadRequestException("Team A percentage and Team B percentage must total 100");
        }
    }

    private BigDecimal calculateAmount(BigDecimal amount, int percentage) {
        return amount
                .multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private WinningTeam normalize(WinningTeam result) {
        if (result == WinningTeam.TEAM_A) {
            return WinningTeam.BOOKER_WIN;
        }
        if (result == WinningTeam.TEAM_B) {
            return WinningTeam.BOOKER_LOSS;
        }
        return result;
    }
}
