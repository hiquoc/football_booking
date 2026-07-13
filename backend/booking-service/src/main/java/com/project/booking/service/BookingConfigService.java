package com.project.booking.service;

import com.project.booking.dto.request.BookingConfigRequest;
import com.project.booking.dto.response.BookingConfigResponse;
import com.project.booking.entity.BookingConfig;

public interface BookingConfigService {
    BookingConfig getConfig();
    BookingConfigResponse getCurrent();
    BookingConfigResponse update(BookingConfigRequest request);
}
