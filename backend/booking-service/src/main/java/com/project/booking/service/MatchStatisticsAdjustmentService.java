package com.project.booking.service;

import com.project.booking.entity.Booking;
import com.project.booking.enums.WinningTeam;

public interface MatchStatisticsAdjustmentService {
    void adjustForResultChange(Booking booking, WinningTeam previousResult, WinningTeam nextResult);
}
