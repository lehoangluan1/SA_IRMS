package SA.irms.identity.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.json.JsonSupport;

@Repository
class IdentityAuditReadRepository {
    private final JdbcClient jdbcClient;
    private final JsonSupport jsonSupport;

    IdentityAuditReadRepository(JdbcClient jdbcClient, JsonSupport jsonSupport) {
        this.jdbcClient = jdbcClient;
        this.jsonSupport = jsonSupport;
    }

    List<IdentityRepository.AuditRow> searchAudit(
            String searchTerm,
            String entityFilter,
            String actorRole,
            LocalDate startDate,
            LocalDate endDate,
            Integer limit
    ) {
        AuditSearchQuery query = AuditSearchQuery.from(searchTerm, entityFilter, actorRole, startDate, endDate, limit);
        JdbcClient.StatementSpec statement = jdbcClient.sql(query.sql());
        for (AuditSearchQuery.Parameter parameter : query.parameters()) {
            statement = statement.param(parameter.name(), parameter.value());
        }
        return statement.query((rs, rowNum) -> new IdentityRepository.AuditRow(
                        rs.getObject("audit_log_id", UUID.class),
                        timestamp(rs.getTimestamp("recorded_at")),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name"),
                        csvToSet(rs.getString("roles")),
                        rs.getString("action"),
                        rs.getString("entity_type"),
                        rs.getString("entity_id"),
                        rs.getString("correlation_id"),
                        rs.getString("reason"),
                        rs.getBoolean("follow_up"),
                        rs.getString("ip_address"),
                        jsonSupport.readMap(rs.getString("before_payload")),
                        jsonSupport.readMap(rs.getString("after_payload"))
                ))
                .list();
    }

    private Instant timestamp(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private java.util.Set<String> csvToSet(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Set.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private record AuditSearchQuery(String sql, List<Parameter> parameters) {
        static AuditSearchQuery from(
                String searchTerm,
                String entityFilter,
                String actorRole,
                LocalDate startDate,
                LocalDate endDate,
                Integer limit
        ) {
            List<String> where = new ArrayList<>();
            List<Parameter> parameters = new ArrayList<>();
            String normalizedSearch = searchTerm == null || searchTerm.isBlank() ? null : "%" + searchTerm.trim().toLowerCase() + "%";
            if (normalizedSearch != null) {
                where.add("""
                        (lower(a.action) like :searchPattern
                         or lower(a.entity_type) like :searchPattern
                         or lower(a.entity_id) like :searchPattern
                         or lower(a.correlation_id) like :searchPattern
                         or lower(u.display_name) like :searchPattern)
                        """);
                parameters.add(new Parameter("searchPattern", normalizedSearch));
            }
            if (entityFilter != null && !entityFilter.isBlank() && !"all".equalsIgnoreCase(entityFilter)) {
                where.add("""
                        (lower(a.action) like :entityPattern
                         or lower(a.entity_type) = :entityExact)
                        """);
                parameters.add(new Parameter("entityPattern", "%" + entityFilter.trim().toLowerCase() + "%"));
                parameters.add(new Parameter("entityExact", entityFilter.trim().toLowerCase()));
            }
            if (startDate != null) {
                where.add("a.recorded_at >= :recordedFrom");
                parameters.add(new Parameter("recordedFrom", Timestamp.from(startDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())));
            }
            if (endDate != null) {
                where.add("a.recorded_at < :recordedToExclusive");
                parameters.add(new Parameter("recordedToExclusive", Timestamp.from(endDate.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant())));
            }
            if (actorRole != null && !actorRole.isBlank() && !"all".equalsIgnoreCase(actorRole)) {
                where.add("""
                        exists (
                            select 1
                            from user_roles ur2
                            join roles r2 on r2.role_id = ur2.role_id
                            where ur2.user_id = u.user_id
                              and r2.name = :roleFilter
                        )
                        """);
                parameters.add(new Parameter("roleFilter", actorRole));
            }
            parameters.add(new Parameter("limit", limit == null ? 100 : limit));
            String predicate = where.isEmpty() ? "" : "where " + String.join("\n  and ", where);
            String sql = """
                    select a.audit_log_id,
                           a.recorded_at,
                           a.action,
                           a.entity_type,
                           a.entity_id,
                           a.correlation_id,
                           a.reason,
                           a.follow_up,
                           a.ip_address,
                           a.before_payload::text as before_payload,
                           a.after_payload::text as after_payload,
                           u.user_id,
                           u.display_name,
                           coalesce(string_agg(distinct r.name, ',' order by r.name), '') as roles
                    from audit_logs a
                    join users u on u.user_id = a.actor_user_id
                    left join user_roles ur on ur.user_id = u.user_id
                    left join roles r on r.role_id = ur.role_id
                    %s
                    group by a.audit_log_id, a.recorded_at, a.action, a.entity_type, a.entity_id, a.correlation_id,
                             a.reason, a.follow_up, a.ip_address, a.before_payload, a.after_payload,
                             u.user_id, u.display_name
                    order by a.recorded_at desc
                    limit :limit
                    """.formatted(predicate);
            return new AuditSearchQuery(sql, parameters);
        }

        private record Parameter(String name, Object value) {
        }
    }
}
