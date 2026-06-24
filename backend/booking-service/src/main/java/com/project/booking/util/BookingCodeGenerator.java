package com.project.booking.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class BookingCodeGenerator {

    private static final String PREFIX = "BK";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generates a unique booking code in the format: BK-20240612-A3F9
     */
    public static String generate() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String randomPart = generateRandomAlphanumeric(4);
        return PREFIX + "-" + datePart + "-" + randomPart;
    }

    private static String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = ThreadLocalRandom.current().nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
}
