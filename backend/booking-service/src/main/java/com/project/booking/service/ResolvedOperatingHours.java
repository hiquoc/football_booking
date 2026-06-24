package com.project.booking.service;

import java.time.LocalTime;

public record ResolvedOperatingHours(LocalTime openTime, LocalTime closeTime, boolean closed) {
}
