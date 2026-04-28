package SA.irms.identity.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class IdentitySessionRepository {
    private final JdbcClient jdbcClient;
    private final IdentityPrincipalLookup principalLookup;

    IdentitySessionRepository(JdbcClient jdbcClient, IdentityPrincipalLookup principalLookup) {
        this.jdbcClient = jdbcClient;
        this.principalLookup = principalLookup;
    }

    Optional<IdentityRepository.SessionPrincipal> findActiveSessionByTokenHash(String tokenHash) {
        return jdbcClient.sql("""
                        select s.session_id,
                               s.user_id,
                               u.username,
                               u.display_name,
                               s.expires_at,
                               s.last_activity_at
                        from user_sessions s
                        join users u on u.user_id = s.user_id
                        where s.token_hash = :tokenHash
                          and s.status = 'active'
                        """)
                .param("tokenHash", tokenHash)
                .query((rs, rowNum) -> {
                    UUID userId = rs.getObject("user_id", UUID.class);
                    return new IdentityRepository.SessionPrincipal(
                            rs.getObject("session_id", UUID.class),
                            userId,
                            rs.getString("username"),
                            rs.getString("display_name"),
                            principalLookup.findRoleNames(userId),
                            principalLookup.findPermissionCodes(userId),
                            rs.getTimestamp("expires_at").toInstant(),
                            rs.getTimestamp("last_activity_at").toInstant()
                    );
                })
                .optional();
    }

    @Transactional
    void touchSession(UUID sessionId, Instant lastActivityAt) {
        jdbcClient.sql("""
                        update user_sessions
                        set last_activity_at = greatest(last_activity_at, :lastActivityAt)
                        where session_id = :sessionId
                          and status = 'active'
                        """)
                .param("lastActivityAt", toSqlTimestamp(lastActivityAt))
                .param("sessionId", sessionId)
                .update();
    }

    @Transactional
    void terminateSession(UUID sessionId, Instant endedAt) {
        jdbcClient.sql("""
                        update user_sessions
                        set status = 'terminated',
                            ended_at = :endedAt
                        where session_id = :sessionId
                        """)
                .param("endedAt", toSqlTimestamp(endedAt))
                .param("sessionId", sessionId)
                .update();
    }

    @Transactional
    void terminateActiveSessionsForUser(UUID userId, Instant endedAt) {
        jdbcClient.sql("""
                        update user_sessions
                        set status = 'terminated',
                            ended_at = :endedAt
                        where user_id = :userId
                          and status = 'active'
                        """)
                .param("userId", userId)
                .param("endedAt", toSqlTimestamp(endedAt))
                .update();
    }

    @Transactional
    void expireIdleSessions(Instant idleBefore, Instant now) {
        jdbcClient.sql("""
                        update user_sessions
                        set status = 'expired',
                            ended_at = :now
                        where status = 'active'
                          and last_activity_at < :idleBefore
                        """)
                .param("idleBefore", toSqlTimestamp(idleBefore))
                .param("now", toSqlTimestamp(now))
                .update();
    }

    @Transactional
    UUID createSession(UUID sessionId, UUID userId, String tokenHash, Instant startedAt, Instant expiresAt, String deviceId, String ipAddress) {
        jdbcClient.sql("""
                        insert into user_sessions (
                            session_id,
                            user_id,
                            token_hash,
                            started_at,
                            expires_at,
                            last_activity_at,
                            device_id,
                            ip_address,
                            status
                        ) values (
                            :sessionId,
                            :userId,
                            :tokenHash,
                            :startedAt,
                            :expiresAt,
                            :startedAt,
                            :deviceId,
                            :ipAddress,
                            'active'
                        )
                        """)
                .param("sessionId", sessionId)
                .param("userId", userId)
                .param("tokenHash", tokenHash)
                .param("startedAt", toSqlTimestamp(startedAt))
                .param("expiresAt", toSqlTimestamp(expiresAt))
                .param("deviceId", deviceId)
                .param("ipAddress", ipAddress)
                .update();
        return sessionId;
    }

    private Timestamp toSqlTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
