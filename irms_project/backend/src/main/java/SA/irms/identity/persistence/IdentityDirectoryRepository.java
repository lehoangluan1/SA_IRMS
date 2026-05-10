package SA.irms.identity.persistence;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.identity.application.port.out.IdentityDirectoryRepositoryPort;

@Repository
public class IdentityDirectoryRepository implements IdentityDirectoryRepositoryPort {
    private final JdbcClient jdbcClient;

    public IdentityDirectoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Map<UUID, String> findDisplayNames(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinctIds = userIds.stream().distinct().toList();
        Map<UUID, String> names = new LinkedHashMap<>();
        jdbcClient.sql("""
                        select user_id, display_name
                        from users
                        where user_id in (:userIds)
                        """)
                .param("userIds", distinctIds)
                .query((rs, rowNum) -> Map.entry(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name")
                ))
                .list()
                .forEach(entry -> names.put(entry.getKey(), entry.getValue()));
        return Map.copyOf(names);
    }

    @Override
    public Optional<UUID> findFirstActiveUserIdByRolePriority(List<String> rolePriority) {
        if (rolePriority == null || rolePriority.isEmpty()) {
            return Optional.empty();
        }
        String orderClause = buildRolePriorityOrder(rolePriority);
        return jdbcClient.sql("""
                        select u.user_id
                        from users u
                        join user_roles ur on ur.user_id = u.user_id
                        join roles r on r.role_id = ur.role_id
                        where u.status = 'active'
                          and r.name in (:roles)
                        order by %s, u.display_name
                        limit 1
                        """.formatted(orderClause))
                .param("roles", rolePriority)
                .query(UUID.class)
                .optional();
    }

    private String buildRolePriorityOrder(List<String> rolePriority) {
        StringBuilder builder = new StringBuilder("case r.name");
        for (int index = 0; index < rolePriority.size(); index++) {
            builder.append(" when '")
                    .append(rolePriority.get(index).replace("'", "''"))
                    .append("' then ")
                    .append(index);
        }
        builder.append(" else ").append(rolePriority.size()).append(" end");
        return builder.toString();
    }
}
