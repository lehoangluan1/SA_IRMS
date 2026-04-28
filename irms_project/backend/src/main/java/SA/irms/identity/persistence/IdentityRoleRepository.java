package SA.irms.identity.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class IdentityRoleRepository {
    private final JdbcClient jdbcClient;

    IdentityRoleRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<IdentityRepository.RoleRow> findRoles() {
        return jdbcClient.sql("""
                        select role_id, name, scope, description
                        from roles
                        order by name
                        """)
                .query((rs, rowNum) -> new IdentityRepository.RoleRow(
                        rs.getObject("role_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("scope"),
                        rs.getString("description"),
                        findPermissionsForRole(rs.getObject("role_id", UUID.class))
                ))
                .list();
    }

    long countActiveUsersWithRole(String roleName) {
        return jdbcClient.sql("""
                        select count(distinct u.user_id)
                        from users u
                        join user_roles ur on ur.user_id = u.user_id
                        join roles r on r.role_id = ur.role_id
                        where u.status = 'active'
                          and r.name = :roleName
                        """)
                .param("roleName", roleName)
                .query(Long.class)
                .single();
    }

    @Transactional
    void replaceUserRoles(UUID userId, List<UUID> roleIds) {
        jdbcClient.sql("delete from user_roles where user_id = :userId")
                .param("userId", userId)
                .update();
        for (UUID roleId : roleIds) {
            jdbcClient.sql("""
                            insert into user_roles (user_id, role_id)
                            values (:userId, :roleId)
                            """)
                    .param("userId", userId)
                    .param("roleId", roleId)
                    .update();
        }
    }

    private List<String> findPermissionsForRole(UUID roleId) {
        return jdbcClient.sql("""
                        select p.code
                        from role_permissions rp
                        join permissions p on p.permission_id = rp.permission_id
                        where rp.role_id = :roleId
                        order by p.code
                        """)
                .param("roleId", roleId)
                .query(String.class)
                .list();
    }
}
