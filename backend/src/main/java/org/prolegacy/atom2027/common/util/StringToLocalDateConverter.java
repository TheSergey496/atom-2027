package org.prolegacy.atom2027.common.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class StringToLocalDateConverter {
    private static final ZoneId utcZone = ZoneId.of("UTC");

    public static LocalDate convert(String dateString) {
        DateTimeFormatter formatter = null;
        if (dateString.matches("^\\d{8}$")) {
            formatter = DateTimeFormatter.BASIC_ISO_DATE;
        } else if (dateString.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        } else if (dateString.matches("^\\d{4}-\\d{2}-\\d{2}[+-]\\d{2}:\\d{2}$")) {
            formatter = DateTimeFormatter.ISO_OFFSET_DATE;
        } else if (dateString.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.]?\\d*$")) {
            formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        } else if (dateString.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.]?\\d*[+-]\\d{2}:\\d{2}$")) {
            formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        } else if (dateString.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.]?\\d*Z$")) {
            formatter = DateTimeFormatter.ISO_INSTANT.withZone(utcZone);
        }

        return LocalDate.parse(
                dateString,
                Optional.ofNullable(formatter)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown date format for string " + dateString))
        );
    }

}
