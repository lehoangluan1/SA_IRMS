package SA.irms.identity.persistence;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class IdentityPrincipalLookup {
    private final JdbcClient jdbcClient;

    IdentityPrincipalLookup(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Set<String> findRoleNames(UUID userId) {
        return jdbcClient.sql("""
                        select r.name
                        from user_roles ur
                        join roles r on r.role_id = ur.role_id
                        where ur.user_id = :userId
                        order by r.name
                        """)
                .param("userId", userId)
                .query(String.class)
                .list()
                .stream()
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    Set<String> findPermissionCodes(UUID userId) {
        return jdbcClient.sql("""
                        select distinct p.code
                        from user_roles ur
                        join role_permissions rp on rp.role_id = ur.role_id
                        join permissions p on p.permission_id = rp.permission_id
                        where ur.user_id = :userId
                        order by p.code
                        """)
                .param("userId", userId)
                .query(String.class)
                .list()
                .stream()
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
