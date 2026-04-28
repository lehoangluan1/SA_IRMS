package SA.irms.reservation.application;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

final class ReservationTimeSupport {
    private ReservationTimeSupport() {
    }

    static Instant toArrivalInstant(String date, String time) {
        LocalDate localDate = LocalDate.parse(date);
        LocalTime localTime = time.contains(":") && time.length() <= 5
                ? LocalTime.parse(time)
                : LocalTime.parse(time.toUpperCase(), DateTimeFormatter.ofPattern("h:mm a"));
        return localDate.atTime(localTime).toInstant(ZoneOffset.UTC);
    }

    static String formatTime(Instant instant) {
        return DateTimeFormatter.ofPattern("h:mm a")
                .withZone(ZoneOffset.UTC)
                .format(instant);
    }

    static String mapReservationStatus(String status) {
        if ("seated".equals(status)) {
            return "checked_in";
        }
        return status;
    }

    static Timestamp toSqlTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
