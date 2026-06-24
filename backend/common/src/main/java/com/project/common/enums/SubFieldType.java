package com.project.common.enums;

import java.util.Arrays;
import java.util.List;

/** A bookable sub-field configuration belonging to exactly one field type. */
public enum SubFieldType {
    FOOTBALL_5V5(SportType.FOOTBALL),
    FOOTBALL_7V7(SportType.FOOTBALL),
    FOOTBALL_11V11(SportType.FOOTBALL),
    BASKETBALL_HALF_COURT(SportType.BASKETBALL),
    BASKETBALL_FULL_COURT(SportType.BASKETBALL),
    BADMINTON(SportType.BADMINTON),
    VOLLEYBALL(SportType.VOLLEYBALL),
    TENNIS(SportType.TENNIS);

    private final SportType fieldType;

    SubFieldType(SportType fieldType) {
        this.fieldType = fieldType;
    }

    public SportType getFieldType() {
        return fieldType;
    }

    public static List<SubFieldType> forFieldType(SportType fieldType) {
        return Arrays.stream(values()).filter(type -> type.fieldType == fieldType).toList();
    }
}
