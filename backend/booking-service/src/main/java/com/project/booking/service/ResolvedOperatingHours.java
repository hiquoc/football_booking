package com.project.booking.service;

import java.time.LocalTime;

public record ResolvedOperatingHours(LocalTime openTime, LocalTime closeTime, boolean closed, boolean open24Hours) {
    public ResolvedOperatingHours(LocalTime openTime, LocalTime closeTime, boolean closed) {
        this(openTime, closeTime, closed, false);
    }
}
