package SA.irms.identity.persistence;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class IdentityStaffReadRepository {
    private final JdbcClient jdbcClient;

    IdentityStaffReadRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<IdentityRepository.StaffRow> findStaff(String search) {
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase() + "%";
        String predicate = searchPattern == null
                ? ""
                : "where lower(u.display_name) like :searchPattern or lower(u.email) like :searchPattern";
        String sql = """
                select u.user_id,
                       u.display_name,
                       u.email,
                       u.status,
                       u.hire_date,
                       string_agg(distinct r.name, ',' order by r.name) as roles
                from users u
                left join user_roles ur on ur.user_id = u.user_id
                left join roles r on r.role_id = ur.role_id
                %s
                group by u.user_id, u.display_name, u.email, u.status, u.hire_date
                order by u.display_name
                """.formatted(predicate);
        JdbcClient.StatementSpec statement = jdbcClient.sql(sql);
        if (searchPattern != null) {
            statement = statement.param("searchPattern", searchPattern);
        }
        return statement.query((rs, rowNum) -> new IdentityRepository.StaffRow(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("email"),
                        rs.getString("status"),
                        rs.getDate("hire_date").toLocalDate(),
                        csvToSet(rs.getString("roles")),
                        findPermissionCodes(rs.getObject("user_id", UUID.class))
                ))
                .list();
    }

    java.util.Optional<IdentityRepository.StaffRow> findStaffById(UUID userId) {
        return jdbcClient.sql("""
                        select u.user_id,
                               u.display_name,
                               u.email,
                               u.status,
                               u.hire_date,
                               string_agg(distinct r.name, ',' order by r.name) as roles
                        from users u
                        left join user_roles ur on ur.user_id = u.user_id
                        left join roles r on r.role_id = ur.role_id
                        where u.user_id = :userId
                        group by u.user_id, u.display_name, u.email, u.status, u.hire_date
                        """)
                .param("userId", userId)
                .query((rs, rowNum) -> new IdentityRepository.StaffRow(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("email"),
                        rs.getString("status"),
                        rs.getDate("hire_date").toLocalDate(),
                        csvToSet(rs.getString("roles")),
                        findPermissionCodes(rs.getObject("user_id", UUID.class))
                ))
                .optional();
    }

    private Set<String> findPermissionCodes(UUID userId) {
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

    private Set<String> csvToSet(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Set.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
