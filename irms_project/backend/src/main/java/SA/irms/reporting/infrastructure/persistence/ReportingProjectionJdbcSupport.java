package SA.irms.reporting.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;

abstract class ReportingProjectionJdbcSupport {
    protected final JdbcClient jdbcClient;

    ReportingProjectionJdbcSupport(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    protected Instant timestamp(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    protected LocalDate businessDate(EventEnvelope envelope) {
        return envelope.metadata().occurredAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
    }

    protected String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    protected BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }
}
