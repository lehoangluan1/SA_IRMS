package SA.irms.identity.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class IdentityUserAuthRepository {
    private final JdbcClient jdbcClient;
    private final IdentityPrincipalLookup principalLookup;

    IdentityUserAuthRepository(JdbcClient jdbcClient, IdentityPrincipalLookup principalLookup) {
        this.jdbcClient = jdbcClient;
        this.principalLookup = principalLookup;
    }

    Optional<IdentityRepository.UserAccount> findUserAccountByEmail(String email) {
        return jdbcClient.sql("""
                        select user_id, username, email, display_name, password_hash, status
                        from users
                        where lower(email) = lower(:email)
                        """)
                .param("email", email)
                .query(this::mapUserAccount)
                .optional();
    }

    Optional<IdentityRepository.UserAccount> findUserAccountById(UUID userId) {
        return jdbcClient.sql("""
                        select user_id, username, email, display_name, password_hash, status
                        from users
                        where user_id = :userId
                        """)
                .param("userId", userId)
                .query(this::mapUserAccount)
                .optional();
    }

    @Transactional
    void updateLastLogin(UUID userId, Instant lastLoginAt) {
        jdbcClient.sql("""
                        update users
                        set last_login_at = :lastLoginAt
                        where user_id = :userId
                        """)
                .param("lastLoginAt", lastLoginAt == null ? null : Timestamp.from(lastLoginAt))
                .param("userId", userId)
                .update();
    }

    private IdentityRepository.UserAccount mapUserAccount(ResultSet rs, int rowNum) throws SQLException {
        UUID userId = rs.getObject("user_id", UUID.class);
        return new IdentityRepository.UserAccount(
                userId,
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("password_hash"),
                rs.getString("status"),
                principalLookup.findRoleNames(userId),
                principalLookup.findPermissionCodes(userId)
        );
    }
}
