package SA.irms.reservation.application.support;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class ReservationTimeSupport {
    private ReservationTimeSupport() {
    }

    public static Instant toArrivalInstant(String date, String time) {
        LocalDate localDate = LocalDate.parse(date);
        LocalTime localTime = time.contains(":") && time.length() <= 5
                ? LocalTime.parse(time)
                : LocalTime.parse(time.toUpperCase(), DateTimeFormatter.ofPattern("h:mm a"));
        return localDate.atTime(localTime).toInstant(ZoneOffset.UTC);
    }

    public static String formatTime(Instant instant) {
        return DateTimeFormatter.ofPattern("h:mm a")
                .withZone(ZoneOffset.UTC)
                .format(instant);
    }

    public static String mapReservationStatus(String status) {
        if ("seated".equals(status)) {
            return "checked_in";
        }
        return status;
    }

    public static Timestamp toSqlTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
