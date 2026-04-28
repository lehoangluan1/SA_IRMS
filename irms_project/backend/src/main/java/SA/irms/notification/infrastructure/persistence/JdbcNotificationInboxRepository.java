package SA.irms.notification.infrastructure.persistence;
import SA.irms.notification.application.view.NotificationViews.NotificationView;

import SA.irms.notification.application.port.out.NotificationInboxRepository;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import SA.irms.common.error.NotFoundException;
import SA.irms.common.security.AuthenticatedUser;

@Repository
public class JdbcNotificationInboxRepository implements NotificationInboxRepository {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public JdbcNotificationInboxRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public List<NotificationView> loadInbox(AuthenticatedUser user) {
        QuerySpec querySpec = buildVisibilityQuery(user, false);
        return jdbcClient.sql("""
                        select message_id,
                               type,
                               title,
                               body,
                               priority,
                               status,
                               created_at,
                               read_at,
                               payload::text as payload
                        from notification_messages
                        where channel = 'in_app'
                          and """
                        + querySpec.whereClause()
                        + """
                        order by case when read_at is null then 0 else 1 end,
                                 created_at desc
                        limit 25
                        """)
                .params(querySpec.params())
                .query((rs, rowNum) -> new NotificationView(
                        rs.getObject("message_id", UUID.class),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("body"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant(),
                        parsePayload(rs.getString("payload"))
                ))
                .list();
    }

    @Transactional
    public NotificationView markRead(UUID messageId, AuthenticatedUser user) {
        QuerySpec querySpec = buildVisibilityQuery(user, true);
        Map<String, Object> params = new LinkedHashMap<>(querySpec.params());
        params.put("messageId", messageId);

        int updated = jdbcClient.sql("""
                        update notification_messages
                        set status = 'read',
                            read_at = coalesce(read_at, :readAt)
                        where channel = 'in_app'
                          and message_id = :messageId
                          and """
                        + querySpec.whereClause())
                .params(params)
                .param("readAt", Timestamp.from(Instant.now()))
                .update();
        if (updated == 0) {
            throw new NotFoundException("Notification was not found.");
        }

        return jdbcClient.sql("""
                        select message_id,
                               type,
                               title,
                               body,
                               priority,
                               status,
                               created_at,
                               read_at,
                               payload::text as payload
                        from notification_messages
                        where message_id = :messageId
                        """)
                .param("messageId", messageId)
                .query((rs, rowNum) -> new NotificationView(
                        rs.getObject("message_id", UUID.class),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("body"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant(),
                        parsePayload(rs.getString("payload"))
                ))
                .single();
    }

    private QuerySpec buildVisibilityQuery(AuthenticatedUser user, boolean includeCurrentUserOnlyWhenExplicit) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();

        predicates.add("recipient_user_id = :userId");
        params.put("userId", user.userId());

        int index = 0;
        for (String role : user.roles()) {
            String key = "role" + index++;
            predicates.add("recipient_role = :" + key);
            params.put(key, role);
        }

        if (user.hasPermission("all") && !includeCurrentUserOnlyWhenExplicit) {
            predicates.add("(recipient_role is null and recipient_user_id is null)");
        }

        return new QuerySpec("(" + String.join(" or ", predicates) + ")", params);
    }

    private Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (IOException exception) {
            return Map.of();
        }
    }

    private record QuerySpec(String whereClause, Map<String, Object> params) {
    }

}
