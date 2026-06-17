package com.vladislav.training.platform.integration.personnel.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Класс {@code PersonnelDateNormalizer}.
 */
public class PersonnelDateNormalizer {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public LocalDate normalize(String rawValue, String columnName, int rowNumber) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawValue.trim(), ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Invalid date value for " + columnName + " at row " + rowNumber + ": " + rawValue,
                e
            );
        }
    }
}
